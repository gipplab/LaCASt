package gov.nist.drmf.interpreter.evaluation;

import com.maplesoft.externalcall.MapleException;
import com.maplesoft.openmaple.Algebraic;
import com.maplesoft.openmaple.Numeric;
import gov.nist.drmf.interpreter.MapleSimplifier;
import gov.nist.drmf.interpreter.MapleTranslator;
import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.maple.common.MapleConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * @author Andre Greiner-Petter
 */
@SuppressWarnings("ALL")
public class SymbolicEvaluator extends AbstractEvaluator<Algebraic> {
    private static final Logger LOG = LogManager.getLogger(SymbolicEvaluator.class.getName());

    private static Path output;

    private MapleTranslator translator;
    private MapleSimplifier simplifier;

    private SymbolicConfig config;

    private HashMap<Integer, String> labelLib;
    private Set<Integer> skips;

    private LinkedList<String>[] lineResults;

    private String overallAss;

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

        String subset = config.getSubSetInterval()[0] + "," + config.getSubSetInterval()[1];
        NumericalConfig.NumericalProperties.KEY_SUBSET.setValue(subset);


        output = config.getOutputPath();
        if (!Files.exists(output)) {
            Files.createFile(output);
        }

        this.labelLib = new HashMap<>();
        this.skips = new HashSet<>();

        String[] skipArr = NumericalEvaluator.LONG_RUNTIME_SKIP.split(",");
        for ( String s : skipArr ) skips.add(Integer.parseInt(s));

        Status.reset();
    }

    @Override
    public void init() throws IOException, MapleException {
        // init translator
        translator = new MapleTranslator();
        translator.init();
        simplifier = translator.getMapleSimplifier();
        super.init(translator, translator);

        //translator.addMapleMemoryObserver(this);
        //MapleListener.setMemoryUsageLimit( MEMORY_NOTIFY_LIMIT_KB );

        overallAss = config.getEntireTestSuiteAssumptions();
    }

    @Override
    public LinkedList<Case> loadTestCases() {
        int[] subset = config.getSubSetInterval();
        HashMap<Integer, String> skippedLinesInfo = new HashMap<>();

        LinkedList<Case> testCases = loadTestCases(
                subset,
                skips,
                config.getDataset(),
                labelLib,
                skippedLinesInfo
        );

        lineResults = new LinkedList[subset[1]];
        for ( Integer i : skippedLinesInfo.keySet() ) {
            lineResults[i] = new LinkedList<>();
            lineResults[i].add(skippedLinesInfo.get(i));
        }

        return testCases;
    }

    @Override
    public EvaluationConfig getConfig() {
        return config;
    }

    @Override
    public HashMap<Integer, String> getLabelLibrary() {
        return labelLib;
    }

    @Override
    public LinkedList<String>[] getLineResults() {
        return lineResults;
    }

    private void setPreviousAssumption() throws ComputerAlgebraSystemEngineException {
        if ( overallAss != null && !overallAss.isEmpty() ){
            String cmd = "assume(" + overallAss + ");";
            LOG.debug("Enter assumption for entire test suite: " + cmd);

            enterEngineCommand(cmd);
//            translator.enterMapleCommand(cmd);

            // todo we may need that?
//            addPreloadScript(cmd);
        }
    }

    public static String[] getPrevCommand( String overAll ){
        String[] pac = new String[2];
        if ( overAll.contains("\\Ferrer") ){
            LOG.info("Found Ferrer function. Enter pre-commands for correct computations in Maple.");
            pac[0] = MapleConstants.ENV_VAR_LEGENDRE_CUT_FERRER;
            pac[0] += System.lineSeparator();
            //pac[0] += FERRER_DEF_ASS + System.lineSeparator();
            pac[1] = MapleConstants.ENV_VAR_LEGENDRE_CUT_LEGENDRE;
            //pac[1] += System.lineSeparator() + RESET;
        } else if ( overAll.contains("\\Legendre") ){
            //pac[0] = LEGENDRE_DEF_ASS;
            //pac[1] = RESET;
        }
        return pac;
    }

    @Override
    public void performSingleTest( Case c ){
        LOG.info("Start test for line: " + c.getLine());
        LOG.info("Test case: " + c);

        if ( lineResults[c.getLine()] == null ){
            lineResults[c.getLine()] = new LinkedList<String>();
        }

        if ( skips.contains(Integer.toString(c.getLine())) ) {
            LOG.info("Skip because long running evaluation.");
            lineResults[c.getLine()].add("Skipped - Long running test");
            Status.SKIPPED.add();
            return;
        }


        try {
//            String mapleAss = null;
//            if ( c.getAssumption() != null ){
//                mapleAss = translator.translateFromLaTeXToMapleClean( c.getAssumption() );
//                LOG.info("Assumption translation: " + mapleAss);
//            }

            enterEngineCommand("reset;");
            setPreviousAssumption();

            String mapleLHS = forwardTranslate( c.getLHS() );
            String mapleRHS = forwardTranslate( c.getRHS() );

            LOG.info("Translate LHS to: " + mapleLHS);
            LOG.info("Translate RHS to: " + mapleRHS);

            String expression = config.getTestExpression( mapleLHS, mapleRHS );

            String[] preAndPostCommands = getPrevCommand( c.getLHS() + ", " + c.getRHS() );

            if ( preAndPostCommands[0] != null ){
                enterEngineCommand(preAndPostCommands[0]);
                LOG.debug("Enter pre-testing commands: " + preAndPostCommands[0]);
            }

            try {
                String arrConstraints = c.getConstraints();
                LOG.debug("Extract constraints: " + arrConstraints);
                if ( arrConstraints != null && arrConstraints.length() > 3 ){
                    arrConstraints = arrConstraints.substring(1, arrConstraints.length()-1);
                    LOG.debug("Enter constraint as assumption: " + arrConstraints);
                    enterEngineCommand("assume(" + arrConstraints + ");");
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
                enterEngineCommand(preAndPostCommands[1]);
                LOG.debug("Enter post-testing commands: " + preAndPostCommands[1]);
            }

            // if one of the above is true -> we are done
            for ( int i = 0; i < success.length; i++ ){
                if ( success[i] ){
                    lineResults[c.getLine()].add("Successful " + Arrays.toString(successStr));
                    Status.SUCCESS.add();
                    return;
//                    return lineResults[c.getLine()].getLast();
                }
            }
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

        return;
//        return c.getLine() + ": " + lineResults[c.getLine()];
    }

    private int gcCaller = 0;

    private int getGcCaller() {
        return gcCaller;
    }

    private void stepGcCaller() {
        this.gcCaller++;
    }

    private void resetGcCaller() {
        this.gcCaller = 0;
    }

    public static void main(String[] args) throws Exception {
        SymbolicEvaluator evaluator = new SymbolicEvaluator();
        evaluator.init();
        LinkedList<Case> tests = evaluator.loadTestCases();
        evaluator.performAllTests(tests);
        evaluator.writeResults();
    }
}
