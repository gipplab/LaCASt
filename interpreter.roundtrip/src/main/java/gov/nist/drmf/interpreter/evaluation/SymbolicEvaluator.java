package gov.nist.drmf.interpreter.evaluation;

import com.maplesoft.externalcall.MapleException;
import com.maplesoft.openmaple.Algebraic;
import com.maplesoft.openmaple.Numeric;
import gov.nist.drmf.interpreter.MapleSimplifier;
import gov.nist.drmf.interpreter.MapleTranslator;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.maple.listener.MapleListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * @author Andre Greiner-Petter
 */
public class SymbolicEvaluator extends NumericalEvaluator {
    private static final Logger LOG = LogManager.getLogger(SymbolicEvaluator.class.getName());

    private static Path output;

    private MapleTranslator translator;
    private MapleSimplifier simplifier;

    private SymbolicConfig config;

    private LinkedList<Case> testCases;

    private HashMap<Integer, String> labelLib;
    private Set<String> skips;

    private LinkedList<String>[] lineResults;

    /**
     * Creates an object for numerical evaluations.
     * Workflow:
     * 1) invoke init();
     * 2) loadTestCases();
     * 3) performTests();
     *
     * @throws IOException
     */
    public SymbolicEvaluator() throws IOException {
        super();

        CaseAnalyzer.ACTIVE_BLUEPRINTS = false; // take raw constraints
        NumericalEvaluator.SKIP_SUC_SYMB = false;

        this.config = new SymbolicConfig();

        NumericalConfig.NumericalProperties.KEY_OUTPUT.setValue(config.getOutputPath().toString());
        NumericalConfig.NumericalProperties.KEY_DATASET.setValue(config.getDataset().toString());
        NumericalConfig.NumericalProperties.KEY_LABELSET.setValue(config.getLabelSet().toString());
        NumericalConfig.NumericalProperties.KEY_DLMF_LINK.setValue(""+config.showDLMFLinks());

        String subset = config.getSubset()[0] + "," + config.getSubset()[1];
        NumericalConfig.NumericalProperties.KEY_SUBSET.setValue(subset);


        output = config.getOutputPath();
        if (!Files.exists(output)) {
            Files.createFile(output);
        }

        this.labelLib = new HashMap<>();
        this.skips = new HashSet<>();

        String[] skipArr = LONG_RUNTIME_SKIP.split(",");
        for ( String s : skipArr ) skips.add(s);

        translator = new MapleTranslator();
        Status.reset();
    }

    @Override
    public void init() throws IOException, MapleException {
        // init translator
        translator.init();
        simplifier = translator.getMapleSimplifier();
        super.setTranslator(translator);

        //translator.addMapleMemoryObserver(this);
        //MapleListener.setMemoryUsageLimit( MEMORY_NOTIFY_LIMIT_KB );

        overallAss = config.getEntireTestSuiteAssumptions();
    }

    private String overallAss;

    private void setPreviousAssumption() throws MapleException {
        if ( overallAss != null && !overallAss.isEmpty() ){
            String cmd = "assume(" + overallAss + ");";
            LOG.debug("Enter assumption for entire test suite: " + cmd);
            translator.enterMapleCommand(cmd);
            addPreloadScript(cmd);
        }
    }

    @Override
    protected String performSingleTest( Case c ){
        LOG.info("Start test for line: " + c.getLine());
        LOG.info("Test case: " + c);

        if ( skips.contains(Integer.toString(c.getLine())) ) {
            LOG.info("Skip because long running evaluation.");
            lineResults[c.getLine()].add("Skipped - Long running test");
            Status.SKIPPED.add();
            return c.getLine() + ": " + lineResults[c.getLine()];
        }

        if ( lineResults == null ){
            String[] old = getLineResults();
            lineResults = new LinkedList[old.length];
            for ( int i = 0; i < old.length; i++ ) {
                lineResults[i] = new LinkedList<String>();
                if ( old[i] != null ) lineResults[i].add(old[i]);
            }
        }

        try {
//            String mapleAss = null;
//            if ( c.getAssumption() != null ){
//                mapleAss = translator.translateFromLaTeXToMapleClean( c.getAssumption() );
//                LOG.info("Assumption translation: " + mapleAss);
//            }

            translator.enterMapleCommand("reset;");
            setPreviousAssumption();

            String mapleLHS = translator.translateFromLaTeXToMapleClean( c.getLHS() );
            String mapleRHS = translator.translateFromLaTeXToMapleClean( c.getRHS() );

            LOG.info("Translate LHS to: " + mapleLHS);
            LOG.info("Translate RHS to: " + mapleRHS);

            String expression = config.getTestExpression( mapleLHS, mapleRHS );

            String[] preAndPostCommands = getPrevCommand( c.getLHS() + ", " + c.getRHS() );

            if ( preAndPostCommands[0] != null ){
                translator.enterMapleCommand(preAndPostCommands[0]);
                LOG.debug("Enter pre-testing commands: " + preAndPostCommands[0]);
            }

            try {
                String arrConstraints = c.getConstraints();
                LOG.debug("Extract constraints: " + arrConstraints);
                if ( arrConstraints != null && arrConstraints.length() > 3 ){
                    arrConstraints = arrConstraints.substring(1, arrConstraints.length()-1);
                    LOG.debug("Enter constraint as assumption: " + arrConstraints);
                    translator.enterMapleCommand("assume(" + arrConstraints + ");");
                }
            } catch ( Exception e ) {
                LOG.warn("Error when parsing constraint => Ignoring Constraint.", e);
            }

            // default values are false
            SymbolicEvaluatorTypes[] type = SymbolicEvaluatorTypes.values();
            String[] successStr = new String[type.length];
            boolean[] success = new boolean[type.length];

            LOG.info(c.getLine() + ": Start simplifications. Expected outcome is "
                    + (config.getExpectationValue() == null ? "numerical" : config.getExpectationValue()) );

            for ( int i = 0; i < type.length; i++ ){
                if ( !type[i].isActivated() ){
                    successStr[i] = type[i].compactToString();
                    continue;
                }

                String testStr = type[i].buildCommand(expression);
                Algebraic a = simplifier.simplify(testStr);
                if ( a == null )
                    throw new IllegalArgumentException("Error in Maple!");

                String aStr = a.toString();
                LOG.info(c.getLine() + ": " + type[i].getShortName() + " - Simplified expression: " + aStr);

                String expectedValue = config.getExpectationValue();
                if ( expectedValue == null ){
                    if ( a instanceof Numeric ){
                        success[i] = true;
                        successStr[i] = type[i].getShortName() + ": " + aStr;
                    } else {
                        successStr[i] = type[i].getShortName() + ": NaN";
                    }
                } else if ( aStr.matches(expectedValue) ) {
                    success[i] = true;
                    successStr[i] = type[i].getShortName() + ": Success";
                } else {
                    successStr[i] = type[i].getShortName() + ": NaN";
                }
            }

            if ( preAndPostCommands[1] != null ){
                translator.enterMapleCommand(preAndPostCommands[1]);
                LOG.debug("Enter post-testing commands: " + preAndPostCommands[1]);
            }

            // if one of the above is true -> we are done
            for ( int i = 0; i < success.length; i++ ){
                if ( success[i] ){
                    lineResults[c.getLine()].add("Successful " + Arrays.toString(successStr));
                    Status.SUCCESS.add();

                    // garbage collection
//                    try {
//                        if ( getGcCaller() % 2 == 0 ) {
//                            translator.forceGC();
//                            resetGcCaller();
//                        } else stepGcCaller();
//                    } catch ( MapleException me ){
//                        LOG.fatal("Cannot call Maple's garbage collector!", me);
//                    }

                    return lineResults[c.getLine()].getLast();
                }
            }

            // garbage collection
//            try {
//                if ( getGcCaller() % 2 == 0 ) {
//                    translator.forceGC();
//                    resetGcCaller();
//                } else stepGcCaller();
//            } catch ( MapleException me ){
//                LOG.fatal("Cannot call Maple's garbage collector!", me);
//            }
            lineResults[c.getLine()].add("Failure " + Arrays.toString(successStr));
            Status.FAILURE.add();
        } catch ( Exception e ){
            LOG.warn("Error for line " + c.getLine() + ", because: " + e.toString(), e);
            if ( e instanceof TranslationException)
                lineResults[c.getLine()].add("Error - " + e.toString());
            else lineResults[c.getLine()].add("Error - " + e.toString() + " [" + c.toString() + "]");
            Status.ERROR.add();
        } finally {
            try {
                if ( getGcCaller() % 1 == 0 ) {
                    translator.forceGC();
                    resetGcCaller();
                } else stepGcCaller();
            } catch ( MapleException me ){
                LOG.fatal("Cannot call Maple's garbage collector!", me);
            }
        }
        return c.getLine() + ": " + lineResults[c.getLine()];
    }

    @Override
    protected String getResults(){
        StringBuffer sb = new StringBuffer();

        sb.append("Overall: ");
        sb.append(Status.buildString());
        sb.append(" for test expression: ");
        sb.append(config.getRawTestExpression());
        sb.append(NL);

        sb.append(Arrays.toString(SymbolicEvaluatorTypes.values()));
        sb.append(NL);

        return buildResults(
                sb.toString(),
                labelLib,
                config.showDLMFLinks(),
                config.getSubset(),
                lineResults
        );
    }

    protected static String buildResults(
            String intro,
            HashMap<Integer, String> labelLib,
            boolean showDLMF,
            int[] limits,
            LinkedList<String>[] lineResults){
        StringBuffer sb = new StringBuffer(intro);

        int start = limits[0];
        int limit = limits[1];

        for ( int i = start; i < lineResults.length && i < limit; i++ ){
            sb.append(i);

            LinkedList<String> lineResult = lineResults[i];
            boolean first = true;
            Character c = 'a';

            if ( lineResults[i] == null ){
                sb.append(": Skipped (is null)").append(NL);
                return sb.toString();
            }

            for ( String s : lineResult ) {
                if ( !first ) {
                    sb.append(i+"-"+c);
                    c++;
                } else first = false;

                String dlmf = labelLib.get(i);
                if ( dlmf != null && showDLMF ){
                    sb.append(" [").append(dlmf).append("]: ");
                } else sb.append(": ");

                sb.append(s);
                sb.append(NL);
            }


        }
        return sb.toString();
    }

    public static void main(String[] args) throws Exception {
        SymbolicEvaluator evaluator = new SymbolicEvaluator();
        evaluator.init();
        evaluator.loadTestCases();
        evaluator.performAllTests();
        evaluator.writeOutput( evaluator.config.getOutputPath() );
    }
}
