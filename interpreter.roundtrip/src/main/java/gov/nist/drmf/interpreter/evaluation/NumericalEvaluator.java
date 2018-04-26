package gov.nist.drmf.interpreter.evaluation;

import com.maplesoft.externalcall.MapleException;
import com.maplesoft.openmaple.Algebraic;
import gov.nist.drmf.interpreter.MapleSimplifier;
import gov.nist.drmf.interpreter.MapleTranslator;
import gov.nist.drmf.interpreter.common.GlobalPaths;
import gov.nist.drmf.interpreter.maple.common.MapleConstants;
import gov.nist.drmf.interpreter.maple.listener.MapleListener;
import gov.nist.drmf.interpreter.maple.translation.MapleInterface;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * @author Andre Greiner-Petter
 */
public class NumericalEvaluator implements Observer {

    private static final Logger LOG = LogManager.getLogger(NumericalEvaluator.class.getName());

    // 0.6GB
    public static final long MEMORY_NOTIFY_LIMIT_KB = 500_000;

    private static DLMFLinker labelLinker;
    private static Path output;

    private MapleTranslator translator;
    private MapleSimplifier simplifier;

    private NumericalConfig config;

    private LinkedList<Case> testCases;

    private HashMap<Integer, String> labelLib;

    private String[] lineResult;

    private String numericalSievesMethod;
    private String numericalSievesMethodRelations;

    private LinkedList<String> mapleScripts;

    /**
     * Creates an object for numerical evaluations.
     * Workflow:
     * 1) invoke init();
     * 2) loadTestCases();
     * 3) performTests();
     *
     * @throws IOException
     */
    public NumericalEvaluator() throws IOException {
        this.config = new NumericalConfig();
        labelLinker = new DLMFLinker(config.getLabelSet());
        labelLib = new HashMap<>();

        translator = new MapleTranslator();
        this.testCases = new LinkedList<>();
        Status.reset();

        this.mapleScripts = new LinkedList<>();
    }

    public void init() throws IOException, MapleException {
        labelLinker = new DLMFLinker(config.getLabelSet());

        output = config.getOutputPath();
        if (!Files.exists(output)) {
            Files.createFile(output);
        }

        // init translator
        translator.init();
        simplifier = translator.getMapleSimplifier();

        translator.addMapleMemoryObserver(this);
        MapleListener.setMemoryUsageLimit( MEMORY_NOTIFY_LIMIT_KB );

        // load special numerical test maple procedure
        String numericalProc = MapleInterface.extractProcedure(GlobalPaths.PATH_MAPLE_NUMERICAL_PROCEDURES);
        translator.enterMapleCommand( numericalProc );
        mapleScripts.add(numericalProc);

        // load expectation of results template
        String expectationTemplate = config.getExpectationTemplate();
        // load numerical sieve
        String sieve_procedure = MapleInterface.extractProcedure( GlobalPaths.PATH_MAPLE_NUMERICAL_SIEVE_PROCEDURE );
        String sieve_procedure_relation = "rel" + sieve_procedure;

        // replace condition placeholder
        this.numericalSievesMethod = MapleInterface.extractNameOfProcedure(sieve_procedure);
        this.numericalSievesMethodRelations = "rel" + numericalSievesMethod;

        sieve_procedure = sieve_procedure.replaceAll(
                NumericalTestConstants.KEY_NUMERICAL_SIEVES_CONDITION,
                expectationTemplate
        );

        sieve_procedure_relation = sieve_procedure_relation.replaceAll(
                NumericalTestConstants.KEY_NUMERICAL_SIEVES_CONDITION,
                "result"
        );

        // load the new script into Maple
        translator.enterMapleCommand(sieve_procedure);
        translator.enterMapleCommand(sieve_procedure_relation);

        mapleScripts.add(sieve_procedure);
        mapleScripts.add(sieve_procedure_relation);
    }

    protected void setTranslator( MapleTranslator translator ){
        this.translator = translator;
    }

    protected void setLabelLinker( Path link ){
        labelLinker = new DLMFLinker(link);
    }

    protected void addPreloadScript(String script){
        this.mapleScripts.add(script);
    }

    private void reloadScripts() throws MapleException {
        for ( String proc : mapleScripts ){
            translator.enterMapleCommand(proc);
        }
    }

    public void loadTestCases() {
        int[] subset = config.getSubset();
        HashMap<Integer, String> skippedLinesInfo = new HashMap<>();

        testCases = loadTestCases( subset, config.getDataset(), labelLib, skippedLinesInfo );

        lineResult = new String[testCases.getLast().getLine()+1];
        for ( Integer i : skippedLinesInfo.keySet() )
            lineResult[i] = skippedLinesInfo.get(i);
    }

    public static LinkedList<Case> loadTestCases(int[] subset, Path dataset, HashMap<Integer, String> labelLib, HashMap<Integer, String> skippedLinesInfo) {
        //int[] subset = config.getSubset();
        LinkedList<Case> testCases = new LinkedList<>();
        int[] currLine = new int[] {0};

        try (BufferedReader br = Files.newBufferedReader(dataset)) {
            Stream<String> lines = br.lines();

            int start = subset[0];
            int limit = subset[1];

            lines   .peek(  l -> currLine[0]++) // line counter
                    .filter(l -> start <= currLine[0] && currLine[0] < limit) // filter by limits
                    .filter(l -> { // filter ' because of ambiguous meanings
                        if (l.contains("'")) {
                            Case c = CaseAnalyzer.analyzeLine(l, currLine[0], labelLinker);
                            LOG.debug("Skip line " + currLine[0] + " because of '.");
                            skippedLinesInfo.put( currLine[0], "Skipped - Because of ambiguous single quote." );
                            if ( c != null ) labelLib.put( c.getLine(), c.getDlmf() );
                            Status.SKIPPED.add();
                            return false;
                        }
                        return true;
                    })
                    .map(l -> {
                        Case c = CaseAnalyzer.analyzeLine(l, currLine[0], labelLinker);
                        if ( c != null ) labelLib.put( c.getLine(), c.getDlmf() );
                        return c;
                    })
                    .filter( c -> {
                        boolean n = Objects.nonNull( c );
                        if ( !n ){
                            skippedLinesInfo.put( currLine[0], "Skipped - Because of NULL element after parsing line." );
                            Status.SKIPPED.add();
                        }
                        return n;
                    })
                    .forEach(testCases::add);

            return testCases;
        } catch( IOException ioe ){
            LOG.fatal("Cannot load dataset!", ioe);
            return null;
        }
    }

    private static final Pattern nullPattern =
            Pattern.compile("[\\s()\\[\\]{}]*0\\.?0*[\\s()\\[\\]{}]*");

    protected String performSingleTest( Case c ){
        try {
            String mapleAss = null;
            if ( c.getAssumption() != null ){
                mapleAss = translator.translateFromLaTeXToMapleClean( c.getAssumption() );
                LOG.info("Assumption translation: " + mapleAss);
            }

            String mapleLHS = translator.translateFromLaTeXToMapleClean( c.getLHS() );
            String mapleRHS = translator.translateFromLaTeXToMapleClean( c.getRHS() );

            LOG.info("Translate LHS to: " + mapleLHS);
            LOG.info("Translate RHS to: " + mapleRHS);

            Matcher nullLHSMatcher = nullPattern.matcher( mapleLHS );
            Matcher nullRHSMatcher = nullPattern.matcher( mapleRHS );
            if ( nullLHSMatcher.matches() ) {
                mapleLHS = "";
            }
            if ( nullRHSMatcher.matches() ) {
                mapleRHS = "";
            }

            String expression = config.getTestExpression( mapleLHS, mapleRHS );

            String[] preAndPostCommands = getPrevCommand( c.getLHS() + ", " + c.getRHS(), mapleAss );

            if ( preAndPostCommands[0] != null ){
                translator.enterMapleCommand(preAndPostCommands[0]);
                LOG.debug("Enter pre-testing commands: " + preAndPostCommands[0]);
            }

            LOG.debug("Start numerical calculations.");
            String resultsName = simplifier.advancedNumericalTest(
                    expression,
                    config.getNumericalValues(),
                    config.getSpecialVariables(),
                    config.getSpecialVariablesValues(),
                    config.getPrecision(),
                    config.getMaximumNumberOfCombs()
            );
            LOG.debug("Finished numerical calculations.");

            if ( preAndPostCommands[1] != null ){
                translator.enterMapleCommand(preAndPostCommands[1]);
                LOG.debug("Enter post-testing commands: " + preAndPostCommands[1]);
            }

            LOG.debug("Start sieving results.");
            String sieveMethod;

            // switch sieve method if it is not an equation
            // in that case, we use the values directly as true/false tests
            if ( c.getRelation().equals( Relations.EQUAL ) ){
                sieveMethod = this.numericalSievesMethod + "(" + resultsName + ");";
            } else {
                sieveMethod = this.numericalSievesMethodRelations + "(" + resultsName + ");";
            }

            Algebraic results = translator.enterMapleCommand(sieveMethod);
            LOG.debug("Finished sieving... save outcome.");

            if ( results instanceof com.maplesoft.openmaple.List ) {
                com.maplesoft.openmaple.List aList = (com.maplesoft.openmaple.List) results;
                int l = aList.length();

                // if l == 0, the list is empty so the test was successful
                if ( l == 0 ){
                    lineResult[c.getLine()] = "Successful";
                    Status.SUCCESS.add();
                } else { // otherwise the list contains errors or simple failures
                    lineResult[c.getLine()] = aList.toString();
                    Status.FAILURE.add();
                }
            }
        } catch ( IllegalArgumentException iae ){
            LOG.warn("Skip test, because " + iae.getMessage());
            lineResult[c.getLine()] = "Skipped - " + iae.getMessage();
            //Status.SKIPPED.add();
        } catch ( Exception e ){
            LOG.warn("Error for line " + c.getLine() + ", because: " + e.toString());
            lineResult[c.getLine()] = "Error - " + e.toString();
            Status.ERROR.add();
        } finally {
            // garbage collection
            try { translator.forceGC(); }
            catch ( MapleException me ){
                LOG.fatal("Cannot call Maple's garbage collector!", me);
            }
        }
        return c.getLine() + ": " + lineResult[c.getLine()];
    }

    protected String[] getLineResults(){
        return lineResult;
    }

    private static final String FERRER_DEF_ASS = "assume(-1 < x, x < 1);";
    private static final String LEGENDRE_DEF_ASS = "assume(1 < x);";
    private static final String RESET = "restart;";

    /**
     * Creates an array for previous and post maple processes.
     * The first element of the array contains maple processes that has to be performed
     * before the numerical test gets performed while the second element
     * contains the reset functions if necessary.
     * @param overAll
     * @param mapleAss
     * @return array of length 2 (0: previous commands, 1: after test commands)
     */
    public static String[] getPrevCommand( String overAll, String mapleAss ){
        String[] pac = new String[2];
        if ( overAll.contains("\\Ferrer") ){
            pac[0] = MapleConstants.ENV_VAR_LEGENDRE_CUT_FERRER;
            pac[0] += System.lineSeparator();
            pac[0] += FERRER_DEF_ASS + System.lineSeparator();
            pac[1] = MapleConstants.ENV_VAR_LEGENDRE_CUT_LEGENDRE;
            pac[1] += System.lineSeparator() + RESET;
        } else if ( overAll.contains("\\Legendre") ){
            pac[0] = LEGENDRE_DEF_ASS;
            pac[1] = RESET;
        }

        if ( pac[0] != null )
            pac[0] += mapleAss == null ?
                    "" :
                    "assume("+mapleAss+");" + System.lineSeparator();
        if ( pac[0] == null && mapleAss != null )
            pac[0] = "assume("+mapleAss+");" + System.lineSeparator();
        return pac;
    }

    private boolean requestedRestart = false;
    private int factor = 1;

    protected void performAllTests(){
        LinkedList<Case> copy = new LinkedList<>();
        while ( !testCases.isEmpty() ){
            if ( requestedRestart ){
                performMapleSessionRestart();
                requestedRestart = false;
                factor++;
                MapleListener.setMemoryUsageLimit( factor*MEMORY_NOTIFY_LIMIT_KB );
            }
            Case c = testCases.removeFirst();
            LOG.info("Start test for line: " + c.getLine());
            performSingleTest(c);
            LOG.info("Finished test for line: " + c.getLine());
            copy.add(c);
        }
        testCases = copy;
    }

    private void performMapleSessionRestart() throws RuntimeException {
        try {
            LOG.info("Try to restart Maple session.");
            translator.restartMapleSession();
            LOG.debug("Reloading procedures...");
            reloadScripts();
            LOG.info("Successfully restarted Maple session.");
        } catch ( MapleException | IOException e ){
            LOG.fatal("Cannot restart maple session!");
            throw new RuntimeException("Restart maple session failed.", e);
        }
    }

    public final static String NL = System.lineSeparator();

    protected String getResults(){
        StringBuffer sb = new StringBuffer();

        sb.append("Overall: ");
        sb.append(Status.buildString());
        sb.append(" for test expression: ");
        sb.append(config.getRawTestExpression());
        sb.append(NL);

        return buildResults(
                sb.toString(),
                labelLib,
                config.showDLMFLinks(),
                config.getSubset(),
                lineResult
        );
    }

    protected static String buildResults(
            String intro,
            HashMap<Integer, String> labelLib,
            boolean showDLMF,
            int[] limits,
            String[] lineResults){
        StringBuffer sb = new StringBuffer(intro);

        int start = limits[0];
        int limit = limits[1];

        for ( int i = start; i < lineResults.length && i < limit; i++ ){
            sb.append(i);
            String dlmf = labelLib.get(i);

            if ( dlmf != null && showDLMF ){
                sb.append(" [").append(dlmf).append("]: ");
            } else sb.append(": ");

            if ( lineResults[i] == null ){
                sb.append("Skipped");
            } else sb.append(lineResults[i]);
            sb.append(NL);
        }
        return sb.toString();
    }

    protected void writeOutput( Path output ) throws IOException {
        String results = getResults();
        Files.write( output, results.getBytes() );
    }

    public static void main(String[] args) throws Exception{
        NumericalEvaluator evaluator = new NumericalEvaluator();
        evaluator.init();
        evaluator.loadTestCases();
        evaluator.performAllTests();
        evaluator.writeOutput( evaluator.config.getOutputPath() );
        //System.out.println(evaluator.performSingleTest( evaluator.testCases.getFirst() ));
    }

    @Override
    public void update(Observable o, Object arg) {
        LOG.info("Observed memory limit was reached. Restart maple session soon!");
        requestedRestart = true;
    }
}
