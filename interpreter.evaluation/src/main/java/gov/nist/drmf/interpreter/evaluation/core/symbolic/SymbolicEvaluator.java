package gov.nist.drmf.interpreter.evaluation.core.symbolic;

import gov.nist.drmf.interpreter.common.cas.ICASEngineSymbolicEvaluator;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.core.DLMFTranslator;
import gov.nist.drmf.interpreter.evaluation.core.translation.MathematicaTranslator;
import gov.nist.drmf.interpreter.evaluation.common.Case;
import gov.nist.drmf.interpreter.evaluation.common.CaseAnalyzer;
import gov.nist.drmf.interpreter.evaluation.common.Status;
import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationExceptionReason;
import gov.nist.drmf.interpreter.common.cas.IComputerAlgebraSystemEngine;
import gov.nist.drmf.interpreter.cas.constraints.IConstraintTranslator;
import gov.nist.drmf.interpreter.evaluation.core.*;
import gov.nist.drmf.interpreter.evaluation.core.numeric.NumericalConfig;
import gov.nist.drmf.interpreter.evaluation.core.numeric.NumericalEvaluator;
import gov.nist.drmf.interpreter.maple.common.MapleConstants;
import gov.nist.drmf.interpreter.maple.extension.MapleInterface;
import gov.nist.drmf.interpreter.maple.extension.Simplifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * @author Andre Greiner-Petter
 */
@SuppressWarnings({"WeakerAccess", "unchecked"})
public class SymbolicEvaluator<T> extends AbstractSymbolicEvaluator<T> {
    private static final Logger LOG = LogManager.getLogger(SymbolicEvaluator.class.getName());

    private SymbolicConfig config;

    private HashMap<Integer, String> labelLib;
    private Set<ID> skips;
    private Set<Integer> idSkips;

    private LinkedList<String>[] lineResults;

    private String overallAss;

    private String[] defaultPrevAfterCmds;

    /**
     * Creates an object for numerical evaluations.
     * Workflow:
     * 1) invoke init();
     * 2) loadTestCases();
     * 3) performTests();
     */
    public SymbolicEvaluator(
            IConstraintTranslator forwardTranslator,
            IComputerAlgebraSystemEngine<T> engine,
            ICASEngineSymbolicEvaluator<T> symbolicEvaluator,
            ISymbolicTestCases[] testCases,
            String[] defaultPrevAfterCmds
    ) throws IOException {
        super( forwardTranslator, engine, symbolicEvaluator, testCases );

        CaseAnalyzer.ACTIVE_BLUEPRINTS = false; // take raw constraints

        this.config = new SymbolicConfig();

        NumericalConfig.NumericalProperties.KEY_OUTPUT.setValue(config.getOutputPath().toString());
        NumericalConfig.NumericalProperties.KEY_DATASET.setValue(config.getDataset().toString());
//        NumericalConfig.NumericalProperties.KEY_LABELSET.setValue(config.getLabelSet().toString());
        NumericalConfig.NumericalProperties.KEY_DLMF_LINK.setValue(""+config.showDLMFLinks());

        String subset = config.getSubSetInterval()[0] + "," + config.getSubSetInterval()[1];
        NumericalConfig.NumericalProperties.KEY_SUBSET.setValue(subset);

        Path output = config.getOutputPath();
        if (!Files.exists(output)) {
            Files.createFile(output);
        }

        this.labelLib = new HashMap<>();
        this.skips = new HashSet<>();
        this.idSkips = new HashSet<>();
        this.defaultPrevAfterCmds = defaultPrevAfterCmds;

        String[] skipArr = NumericalEvaluator.LONG_RUNTIME_SKIP.split(",");
        for ( String s : skipArr ) {
            skips.add(new ID(Integer.parseInt(s)));
            idSkips.add(Integer.parseInt(s));
        }

        Status.reset();
    }

//    @Override
    public void init() {
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
//            String cmd = "assume(" + overallAss + ");";
            String cmd = "And[" + overallAss + "]";
            LOG.debug("Enter assumption for entire test suite: " + cmd);

//            enterEngineCommand(cmd);
//            translator.enterMapleCommand(cmd);

            // todo we may need that?
//            addPreloadScript(cmd);
        }
    }

    @Override
    public void performAllTests(LinkedList<Case> cases) {
        try {
            setPreviousAssumption();
            super.performAllTests(cases);
        } catch ( ComputerAlgebraSystemEngineException casee ) {
            LOG.error("Cannot perform assumptions.", casee);
        }
    }

    public static String[] getMaplePrevAfterCommands() {
        String[] pac = new String[2];
        pac[0] = MapleConstants.ENV_VAR_LEGENDRE_CUT_FERRER;
        pac[0] += System.lineSeparator();
        //pac[0] += FERRER_DEF_ASS + System.lineSeparator();
        pac[1] = MapleConstants.ENV_VAR_LEGENDRE_CUT_LEGENDRE;
        return pac;
    }

    public String[] checkPrevCommand( String caseStr ){
        if ( caseStr.contains("\\Ferrer") ){
            return this.defaultPrevAfterCmds;
//        } else if ( overAll.contains("\\Legendre") ){
            //pac[0] = LEGENDRE_DEF_ASS;
            //pac[1] = RESETIntegrate[Divide[1, (1+t^2)^(1/2)], {t, 0, 1/ z}];
        }
        return null;
    }

    @Override
    public void performSingleTest( Case c ){
        LOG.info("Start test for line: " + c.getLine());
        LOG.info("Test case: " + c);

        if ( lineResults[c.getLine()] == null ){
            lineResults[c.getLine()] = new LinkedList();
        }

        if ( c instanceof AbstractEvaluator.DummyCase ) {
            lineResults[c.getLine()].add("Skip - ");
            Status.SKIPPED.add();
            return;
        }

//        if ( skips.contains(Integer.toString(c.getLine())) ) {
//            LOG.info("Skip because long running evaluation.");
//            lineResults[c.getLine()].add("Skipped - Long running test");
//            Status.SKIPPED.add();
//            return;
//        }

        try {
//            String mapleAss = null;
//            if ( c.getAssumption() != null ){
//                mapleAss = translator.translateFromLaTeXToMapleClean( c.getAssumption() );
//                LOG.info("Assumption translation: " + mapleAss);
//            }

//            enterEngineCommand("reset;");
//            setPreviousAssumption();

            Status.STARTED_TEST_CASES.add();
            String mapleLHS = forwardTranslate( c.getLHS(), c.getEquationLabel() );
            String mapleRHS = forwardTranslate( c.getRHS(), c.getEquationLabel() );

            LOG.info("Translate LHS to: " + mapleLHS);
            LOG.info("Translate RHS to: " + mapleRHS);
            Status.SUCCESS_TRANS.add();

            String expression = config.getTestExpression( mapleLHS, mapleRHS );

            String[] preAndPostCommands = checkPrevCommand( c.getLHS() + ", " + c.getRHS() );

            if ( preAndPostCommands != null ){
                enterEngineCommand(preAndPostCommands[0]);
                LOG.debug("Enter pre-testing commands: " + preAndPostCommands[0]);
            }

            String arrConstraints = null;
            try {
                List<String> consList = c.getConstraints(this.getThisConstraintTranslator(), c.getEquationLabel());
                LOG.debug("Extract constraints: " + consList);
                if ( consList != null ) {
                    arrConstraints = getCASListRepresentation(consList);
                }
            } catch ( Exception e ) {
                LOG.warn("Error when parsing constraint => Ignoring Constraint.", e);
            }

            // default values are false
            ISymbolicTestCases[] type = getSymbolicTestCases();
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

                T res = simplify( testStr, arrConstraints );
                if ( res == null ) {
                    throw new IllegalArgumentException("Error in CAS!");
                }

                if ( isAbortedResult(res) ) {
                    success[i] = false;
                    successStr[i] = type[i].getShortName() + ": Aborted";
                } else {
    //                String strRes = res.toString();
                    LOG.info(c.getLine() + ": " + type[i].getShortName() + " - Simplified expression: " + res);

                    if ( validOutCome(res, config.getExpectationValue()) ) {
                        success[i] = true;
                        successStr[i] = type[i].getShortName() + ": " + res;
                    } else {
                        success[i] = false;
                        successStr[i] = type[i].getShortName() + ": NaN";
                    }
                }

            }

            if ( preAndPostCommands != null ){
                enterEngineCommand(preAndPostCommands[1]);
                LOG.debug("Enter post-testing commands: " + preAndPostCommands[1]);
            }

            // if one of the above is true -> we are done
            for ( int i = 0; i < success.length; i++ ){
                if ( success[i] ){
                    lineResults[c.getLine()].add("Successful " + Arrays.toString(successStr));
                    Status.SUCCESS.add();
                    Status.SUCCESS_SYMB.add();
                    return;
//                    return lineResults[c.getLine()].getLast();
                }
            }
            lineResults[c.getLine()].add("Failure " + Arrays.toString(successStr));
            Status.FAILURE.add();
        } catch ( Exception e ){
            LOG.warn("Error for line " + c.getLine() + ", because: " + e.toString(), e);
            if ( e instanceof TranslationException){
                lineResults[c.getLine()].add("Error - " + e.toString());
                TranslationException te = (TranslationException)e;
                if (
                        te.getReason().equals( TranslationExceptionReason.MISSING_TRANSLATION_INFORMATION ) ||
                        te.getReason().equals( TranslationExceptionReason.LATEX_MACRO_ERROR ) ||
                        te.getReason().equals( TranslationExceptionReason.UNKNOWN_OR_MISSING_ELEMENT )
                ) {
                    Status.MISSING.add();
                    if ( te.getReasonObj() != null )
                        addMissingMacro(te.getReasonObj().toString());
                } else Status.ERROR.add();
            } else {
                lineResults[c.getLine()].add("Error - " + e.toString() + " [" + c.toString() + "]");
                Status.ERROR.add();
            }
        } finally {
            try {
                if ( getGcCaller() % 1 == 0 ) {
                    forceGC();
                    resetGcCaller();
                } else stepGcCaller();
            } catch ( ComputerAlgebraSystemEngineException me ){
                LOG.fatal("Cannot call CAS garbage collector!", me);
            }
        }
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

    public static SymbolicEvaluator createStandardMapleEvaluator() throws Exception {
        DLMFTranslator dlmfTranslator = new DLMFTranslator(Keys.KEY_MAPLE);
        MapleInterface mapleInterface = MapleInterface.getUniqueMapleInterface();
        Simplifier simplifier = new Simplifier();

        SymbolicEvaluator evaluator = new SymbolicEvaluator(
                dlmfTranslator,
                mapleInterface,
                simplifier,
                SymbolicMapleEvaluatorTypes.values(),
                SymbolicEvaluator.getMaplePrevAfterCommands()
        );

        evaluator.init();
        return evaluator;
    }

    public static SymbolicEvaluator createStandardMathematicaEvaluator() throws Exception {
        MathematicaTranslator translator = new MathematicaTranslator();
        translator.init();

        SymbolicEvaluator evaluator = new SymbolicEvaluator(
                translator,
                translator,
                translator,
                SymbolicMathematicaEvaluatorTypes.values(),
                null
        );

        evaluator.init();
        return evaluator;
    }

    private static void startTestAndWriteResults( SymbolicEvaluator evaluator ) throws IOException {
        LinkedList<Case> tests = evaluator.loadTestCases();
        evaluator.performAllTests(tests);
        evaluator.writeResults();
    }

    public static void main(String[] args) throws Exception {
        SymbolicEvaluator evaluator = null;
        if ( args == null || args.length < 1 ){
            System.out.println("Start Mathematica Evaluator");
            evaluator = createStandardMathematicaEvaluator();
        } else if ( args[0].matches("--?mathematica") ) {
            System.out.println("Start Mathematica Evaluator");
            evaluator = createStandardMathematicaEvaluator();
        } else if ( args[0].matches("--?maple") ) {
            System.out.println("Start Maple Evaluator");
            evaluator = createStandardMapleEvaluator();
        } else {
            System.out.println("Choose maple or mathematica (e.g., argument -math)");
            return;
        }

//        SymbolicEvaluator evaluator = createStandardMapleEvaluator();
//        SymbolicEvaluator evaluator = createStandardMathematicaEvaluator();
        startTestAndWriteResults(evaluator);
    }
}
