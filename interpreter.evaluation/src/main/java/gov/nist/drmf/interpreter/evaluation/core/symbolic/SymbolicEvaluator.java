package gov.nist.drmf.interpreter.evaluation.core.symbolic;

import gov.nist.drmf.interpreter.common.cas.ICASEngineSymbolicEvaluator;
import gov.nist.drmf.interpreter.common.eval.*;
import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import gov.nist.drmf.interpreter.common.exceptions.InitTranslatorException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationExceptionReason;
import gov.nist.drmf.interpreter.common.eval.SymbolicCalculation;
import gov.nist.drmf.interpreter.common.eval.SymbolicResult;
import gov.nist.drmf.interpreter.core.api.DLMFTranslator;
import gov.nist.drmf.interpreter.evaluation.common.Case;
import gov.nist.drmf.interpreter.evaluation.common.CaseAnalyzer;
import gov.nist.drmf.interpreter.evaluation.common.Status;
import gov.nist.drmf.interpreter.evaluation.core.AbstractEvaluator;
import gov.nist.drmf.interpreter.maple.MapleConnector;
import gov.nist.drmf.interpreter.mathematica.MathematicaConnector;
import gov.nist.drmf.interpreter.mathematica.extension.MathematicaInterface;
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
public class SymbolicEvaluator extends AbstractSymbolicEvaluator {
    private static final String MANUAL_SKIP_IDS = "7731";

    private static final Logger LOG = LogManager.getLogger(SymbolicEvaluator.class.getName());

    private SymbolicalConfig config;

    private HashMap<Integer, String> labelLib;
    private Set<ID> skips;
    private Set<Integer> idSkips;

    private LinkedList<String>[] lineResults;

    private String overallAss;

    private String[] defaultPrevAfterCmds;

    private boolean mathematica = false;

    private final double expectedResult;

    /**
     * Creates an object for numerical evaluations.
     * Workflow:
     * 1) invoke init();
     * 2) loadTestCases();
     * 3) performTests();
     */
    public SymbolicEvaluator(
            NativeComputerAlgebraInterfaceBuilder casBuilder
    ) throws IOException, InitTranslatorException {
        super( new DLMFTranslator(casBuilder.getLanguageKey()), casBuilder.getCASEngine(), casBuilder.getSymbolicEvaluator(), casBuilder.getDefaultSymbolicTestCases() );

        CaseAnalyzer.ACTIVE_BLUEPRINTS = false; // take raw constraints

        this.config = new SymbolicalConfig(casBuilder.getDefaultSymbolicTestCases());
        casBuilder.getSymbolicEvaluator().setTimeout(config.getTimeout());
        super.setTimeoutSeconds(config.getTimeout());

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

        String[] skipArr = SymbolicEvaluator.MANUAL_SKIP_IDS.split(",");
        for ( String s : skipArr ) {
            skips.add(new ID(Integer.parseInt(s)));
            idSkips.add(Integer.parseInt(s));
        }

        Status.reset();
        mathematica = (casBuilder.getCASEngine() instanceof MathematicaInterface);
        expectedResult = Double.parseDouble(config.getExpectationValue());
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

    private void setGlobalAssumption() throws ComputerAlgebraSystemEngineException {
        if ( mathematica ) {
            LOG.warn("Somehow, Mathematica seems to dislike assumptions for symbolic simplifications. Hence we ignore " +
                    "the defined assumptions until we found a solution to this!");
            return;
        }
        if ( overallAss != null && !overallAss.isEmpty() ){
            String[] ass = overallAss.split(" \\|\\| ");
            String[] transAss = this.getThisConstraintTranslator().translateEachConstraint(ass);
            super.setGlobalSymbolicAssumptions(List.of(transAss));
        }
    }

    @Override
    public void performAllTests(LinkedList<Case> cases) {
        try {
            setGlobalAssumption();
            super.performAllTests(cases);
        } catch ( ComputerAlgebraSystemEngineException casee ) {
            LOG.error("Cannot perform assumptions.", casee);
        }
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
    public void performSingleTest( Case c ) {
        LOG.info("Start test for line: " + c.getLine());
        LOG.info("Test case: " + c);

        if (lineResults[c.getLine()] == null) {
            lineResults[c.getLine()] = new LinkedList();
        }

        if (c instanceof AbstractEvaluator.DummyCase) {
            lineResults[c.getLine()].add("Skipped - Invalid subtest");
            Status.SKIPPED.add();
            return;
        }

        LOG.info("Replacing defined symbols.");
        c.replaceSymbolsUsed(super.getSymbolDefinitionLibrary());
        LOG.info("Final Test case: " + c);

        // first translations
        String lhs = null, rhs = null;
        try {
            Status.STARTED_TEST_CASES.add();
            startRememberPackages();
            lhs = forwardTranslate(c.getLHS(), c.getEquationLabel()).getTranslatedExpression();
            rhs = forwardTranslate(c.getRHS(), c.getEquationLabel()).getTranslatedExpression();
            stopRememberPackages();

            LOG.info("Translate LHS to: " + lhs);
            LOG.info("Translate RHS to: " + rhs);
            Status.SUCCESS_TRANS.add();
        } catch (TranslationException te) {
            LOG.warn("Error for line " + c.getLine() + ", because: " + te, te);
            if ( (te.getReason().equals(TranslationExceptionReason.MISSING_TRANSLATION_INFORMATION) ||
                            te.getReason().equals(TranslationExceptionReason.LATEX_MACRO_ERROR) ||
                            te.getReason().equals(TranslationExceptionReason.UNKNOWN_OR_MISSING_ELEMENT)
                ) && te.getReasonObj() != null
            ) {
                Status.MISSING.add();
                lineResults[c.getLine()].add("Missing Macro Error - " + te.toString());
                addMissingMacro(te.getReasonObj().toString());
            } else {
                Status.ERROR_TRANS.add();
                lineResults[c.getLine()].add("Translation Error - " + te.toString());
            }
            return;
        } catch (Exception | Error e) {
            lineResults[c.getLine()].add("Translation Error - " + e.toString() + " [" + c.toString() + "]");
            Status.ERROR_TRANS.add();
            return;
        }

        if (lhs == null || rhs == null) return;

        String expression = config.getTestExpression(lhs, rhs);

        String[] preAndPostCommands = checkPrevCommand(c.getLHS() + ", " + c.getRHS());

        if (preAndPostCommands != null) {
            try {
                enterEngineCommand(preAndPostCommands[0]);
                LOG.debug("Enter pre-testing commands: " + preAndPostCommands[0]);
            } catch (ComputerAlgebraSystemEngineException e) {
                LOG.warn("Unable to enter pre/post commands. Ignoring it and continue.", e);
            }
        }

        String arrConstraints = null;
        try {
            List<String> consList = c.getConstraints(this.getThisConstraintTranslator(), c.getEquationLabel());
            LOG.debug("Extract constraints: " + consList);
            if (consList != null) {
                arrConstraints = getCASListRepresentation(consList);
            }
        } catch (Exception e) {
            LOG.warn("Error when parsing constraints => Ignoring Constraint: " + c.getRawConstraint());
        }

        // default values are false
        ISymbolicTestCases[] type = getSymbolicTestCases();
        String[] successStr = new String[type.length];
        boolean[] success = new boolean[type.length];
        boolean[] errors = new boolean[type.length];
        boolean[] aborted = new boolean[type.length];

        LOG.info(c.getLine() + ": Start simplifications. Expected outcome is "
                + (config.getExpectationValue() == null ? "numerical" : config.getExpectationValue()));

        ICASEngineSymbolicEvaluator evaluator = super.getSymbolicEvaluator();
        SymbolicalTest test = new SymbolicalTest(
                config,
                getThisConstraintTranslator(),
                expression,
                type
        );

        SymbolicResult result = evaluator.performSymbolicTest(test);

        if (preAndPostCommands != null) {
            try {
                enterEngineCommand(preAndPostCommands[1]);
                LOG.debug("Enter post-testing commands: " + preAndPostCommands[1]);
            } catch (ComputerAlgebraSystemEngineException e) {
                LOG.warn("Unable to enter post-testinc commands: " + preAndPostCommands[1], e);
            }
        }

        boolean allAborted = true;
        for (SymbolicCalculation sc : result.getTestCalculations()) {
            allAborted &= sc.wasAborted();
            if ( sc.wasSuccessful() ) {
                if ( sc.isWasConditionallySuccessful() ) {
                    lineResults[c.getLine()].add("Successful under condition " + result.printCalculations());
                    Status.SUCCESS_UNDER_EXTRA_CONDITION.add();
                } else {
                    lineResults[c.getLine()].add("Successful " + result.printCalculations());
                    Status.SUCCESS_SYMB.add();
                }
                return;
            }
        }

        if (TestResultType.ERROR.equals(result.getTestResultType())) {
            lineResults[c.getLine()].add("All Errors: " + Arrays.toString(successStr));
            Status.ERROR.add();
        } else if (allAborted) {
            lineResults[c.getLine()].add("All Aborted: " + Arrays.toString(successStr));
            Status.ABORTED.add();
        } else {
            lineResults[c.getLine()].add("Failure " + Arrays.toString(successStr));
            Status.FAILURE.add();
        }

//        for (int i = 0; i < type.length; i++) {
//            aborted[i] = false;
//            if (!type[i].isActivated()) {
//                successStr[i] = type[i].compactToString();
//                continue;
//            }
//
//            String testStr = type[i].buildCommand(expression);
//
//            try {
//                T res = simplify(testStr, arrConstraints);
//                if (res == null) {
//                    LOG.warn("CAS return NULL for: " + testStr);
//                    throw new IllegalArgumentException("Error in CAS!");
//                }
//
//                if (isAbortedResult(res)) {
//                    success[i] = false;
//                    aborted[i] = true;
//                    successStr[i] = type[i].getShortName() + ": Aborted";
//                    LOG.info(c.getLine() + " [" + type[i].getShortName() + "] Computation aborted.");
//                } else {
//                    // String strRes = res.toString();
//                    LOG.info(c.getLine() + " [" + type[i].getShortName() + "] Simplified expression: " + res);
//
//                    if (validOutCome(res, expectedResult)) {
//                        success[i] = true;
//                        successStr[i] = type[i].getShortName() + ": " + res;
//                    } else if ( isConditionallyValid(res, expectedResult) ) {
//                        String condition = getCondition(res);
//                        success[i] = true;
//                        successStr[i] = type[i].getShortName() + ": " + expectedResult + " under condition: " + condition;
//                        LOG.warn(c.getLine() + " [" + type[i].getShortName() + "]: Correct result but under extra conditions: " + condition);
//                    } else {
//                        success[i] = false;
//                        successStr[i] = type[i].getShortName() + ": NaN";
//                    }
//                }
//                errors[i] = false;
//            } catch (ComputerAlgebraSystemEngineException casee) {
//                LOG.error(c.getLine() + ": " + type[i].getShortName() + " - Error in CAS: " + casee.toString(), casee);
//                success[i] = false;
//                successStr[i] = type[i].getShortName() + ": CAS Error (" + casee.getMessage() + ")";
//                errors[i] = true;
//            } catch (IllegalArgumentException iae) {
//                LOG.error(c.getLine() + ": " + type[i].getShortName() + " - Result was null");
//                success[i] = false;
//                successStr[i] = type[i].getShortName() + ": NULL";
//                errors[i] = true;
//            }
//        }

//        if (preAndPostCommands != null) {
//            try {
//                enterEngineCommand(preAndPostCommands[1]);
//                LOG.debug("Enter post-testing commands: " + preAndPostCommands[1]);
//            } catch (ComputerAlgebraSystemEngineException e) {
//                LOG.warn("Unable to enter post-testinc commands: " + preAndPostCommands[1], e);
//            }
//        }
//
//        // if one of the above is true -> we are done
//        boolean allError = true;
//        boolean allAborted = true;
//        for (int i = 0; i < success.length; i++) {
//            if (!errors[i]) allError = false;
//            if (!aborted[i]) allAborted = false;
//            if (success[i]) {
//                if ( successStr[i].contains("condition") ) {
//                    lineResults[c.getLine()].add("Successful under condition " + Arrays.toString(successStr));
//                    Status.SUCCESS_UNDER_EXTRA_CONDITION.add();
//                } else {
//                    lineResults[c.getLine()].add("Successful " + Arrays.toString(successStr));
//                    Status.SUCCESS_SYMB.add();
//                }
//                return;
//            }
//        }
//
//        if (allError) {
//            lineResults[c.getLine()].add("All Errors: " + Arrays.toString(successStr));
//            Status.ERROR.add();
//        } else if (allAborted) {
//            lineResults[c.getLine()].add("All Aborted: " + Arrays.toString(successStr));
//            Status.ABORTED.add();
//        } else {
//            lineResults[c.getLine()].add("Failure " + Arrays.toString(successStr));
//            Status.FAILURE.add();
//        }

        try {
            if (getGcCaller() % 10 == 0) {
                forceGC();
                resetGcCaller();
            } else stepGcCaller();
        } catch (ComputerAlgebraSystemEngineException me) {
            LOG.fatal("Cannot call CAS garbage collector!", me);
        }

//            String mapleAss = null;
//            if ( c.getAssumption() != null ){
//                mapleAss = translator.translateFromLaTeXToMapleClean( c.getAssumption() );
//                LOG.info("Assumption translation: " + mapleAss);
//            }

//            enterEngineCommand("reset;");
//            setPreviousAssumption();
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
        SymbolicEvaluator evaluator = new SymbolicEvaluator(new MapleConnector());
        evaluator.init();
        return evaluator;
    }

    public static SymbolicEvaluator createStandardMathematicaEvaluator() throws Exception {
        SymbolicEvaluator evaluator = new SymbolicEvaluator(new MathematicaConnector());
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
