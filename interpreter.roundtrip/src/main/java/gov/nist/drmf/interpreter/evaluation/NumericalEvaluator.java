package gov.nist.drmf.interpreter.evaluation;

import com.maplesoft.externalcall.MapleException;
import com.maplesoft.openmaple.Algebraic;
import gov.nist.drmf.interpreter.MapleSimplifier;
import gov.nist.drmf.interpreter.MapleTranslator;
import gov.nist.drmf.interpreter.common.GlobalConstants;
import gov.nist.drmf.interpreter.common.GlobalPaths;
import gov.nist.drmf.interpreter.maple.common.MapleConstants;
import gov.nist.drmf.interpreter.maple.translation.MapleInterface;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * @author Andre Greiner-Petter
 */
public class NumericalEvaluator {

    private static final Logger LOG = LogManager.getLogger(NumericalEvaluator.class.getName());

    private static DLMFLinker labelLinker;
    private static Path output;

    private MapleTranslator translator;
    private MapleSimplifier simplifier;

    private NumericalConfig config;

    private LinkedList<Case> testCases;

    private HashMap<Integer, String> labelLib;

    private String[] lineResult;

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

        output = config.getOutputPath();
        if (!Files.exists(output)) {
            Files.createFile(output);
        }

        translator = new MapleTranslator();
        this.testCases = new LinkedList<>();
        Status.reset();
    }

    public void init() throws IOException, MapleException {
        translator.init();
        simplifier = translator.getMapleSimplifier();

        translator.enterMapleCommand(
                MapleInterface.extractProcedure(GlobalPaths.PATH_MAPLE_NUMERICAL_PROCEDURES)
        );
    }

    public void loadTestCases() {
        int[] subset = config.getSubset();
        int[] currLine = new int[] {1};

        try (BufferedReader br = Files.newBufferedReader(config.getDataset())) {
            Stream<String> lines = br.lines();

            if (subset != null) {
                lines = lines
                        .skip(subset[0] - 1)
                        .limit(subset[1]);
                currLine[0] = subset[0];
                Status.SKIPPED.set( subset[0]-1 );
            }

            HashMap<Integer, String> skippedLinesInfo = new HashMap<>();

            lines   .filter(l -> {
                        if (l.contains("'")) {
                            Case c = CaseAnalyzer.analyzeLine(l, currLine[0], labelLinker);
                            LOG.debug("Skip line " + currLine[0] + " because of '.");
                            skippedLinesInfo.put( currLine[0], "Skipped - Because of ambiguous single quote." );
                            if ( c != null ) labelLib.put( c.getLine(), c.getDlmf() );
                            currLine[0]++;
                            Status.SKIPPED.add();
                            return false;
                        }
                        return true;
                    })
                    .map(l -> {
                        Case c = CaseAnalyzer.analyzeLine(l, currLine[0]++, labelLinker);
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
                    .forEach(c -> testCases.add(c));

            lineResult = new String[currLine[0]];
            for ( Integer i : skippedLinesInfo.keySet() )
                lineResult[i] = skippedLinesInfo.get(i);
        } catch( IOException ioe ){
            LOG.fatal("Cannot load dataset!", ioe);
        }
    }

    private static final Pattern nullPattern =
            Pattern.compile("[\\s()\\[\\]{}]*0\\.?0*[\\s()\\[\\]{}]*");

    private String performSingleTest( Case c ){
        try {
            String mapleAss = null;
            if ( c.getAssumption() != null )
                mapleAss = translator.translateFromLaTeXToMapleClean( c.getAssumption() );
            String mapleLHS = translator.translateFromLaTeXToMapleClean( c.getLHS() );
            String mapleRHS = translator.translateFromLaTeXToMapleClean( c.getRHS() );

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

            if ( preAndPostCommands[0] != null )
                translator.enterMapleCommand(preAndPostCommands[0]);

            Algebraic results = simplifier.numericalTest(
                    expression,
                    config.getNumericalValues(),
                    config.getPrecision()
            );

            if ( preAndPostCommands[1] != null )
                translator.enterMapleCommand(preAndPostCommands[1]);

            LinkedList<String> resultsList = new LinkedList<>();

            if ( results instanceof com.maplesoft.openmaple.List ){
                com.maplesoft.openmaple.List aList = (com.maplesoft.openmaple.List) results;
                int l = aList.length();
                for ( int i = 1; i <= l; i++ ){
                    Algebraic e = aList.select(i);
                    if ( e instanceof com.maplesoft.openmaple.List ){
                        com.maplesoft.openmaple.List innerL = (com.maplesoft.openmaple.List)e;
                        String num = innerL.select(1).toString();

                        String testResult = config.getExpectation( num );
                        Algebraic testResultBoolean = translator.enterMapleCommand( testResult );

                        String resBoolean = testResultBoolean.toString();

                        if ( resBoolean.equals( "false" ) ){
                            resultsList.add(e.toString());
                        }
                    }
                }
            }

            if ( resultsList.isEmpty() ){
                lineResult[c.getLine()] = "Successful";
                Status.SUCCESS.add();
            } else {
                lineResult[c.getLine()] = resultsList.toString();
                Status.FAILURE.add();
            }
        } catch ( Exception e ){
            LOG.error("Cannot perform numerical test for line " + c.getLine(), e );
            lineResult[c.getLine()] = "Error - " + e.toString();
            Status.ERROR.add();
        }
        return c.getLine() + ": " + lineResult[c.getLine()];
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
    private String[] getPrevCommand( String overAll, String mapleAss ){
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

    private void performAllTests(){
        LinkedList<Case> copy = new LinkedList<>();
        while ( !testCases.isEmpty() ){
            Case c = testCases.removeFirst();
            performSingleTest(c);
            copy.add(c);
        }
        testCases = copy;
    }

    private final static String NL = System.lineSeparator();

    private String getResults(){
        StringBuffer sb = new StringBuffer();

        sb.append("Overall: ");
        sb.append(Status.buildString());
        sb.append(" for test expression: ");
        sb.append(config.getRawTestExpression());
        sb.append(NL);

        boolean showDLMF = config.showDLMFLinks();

        for ( int i = 1; i < lineResult.length; i++ ){
            sb.append(i);
            String dlmf = labelLib.get(i);

            if ( dlmf != null && showDLMF ){
                sb.append(" [").append(dlmf).append("]: ");
            } else sb.append(": ");

            if ( lineResult[i] == null ){
                sb.append("Skipped");
            } else sb.append(lineResult[i]);
            sb.append(NL);
        }
        return sb.toString();
    }

    private void writeOutput() throws IOException {
        String results = getResults();
        Files.write( output, results.getBytes() );
    }

    public static void main(String[] args) throws Exception{
        NumericalEvaluator evaluator = new NumericalEvaluator();
        evaluator.init();
        evaluator.loadTestCases();
        evaluator.performAllTests();
        evaluator.writeOutput();
        //System.out.println(evaluator.performSingleTest( evaluator.testCases.getFirst() ));
    }
}
