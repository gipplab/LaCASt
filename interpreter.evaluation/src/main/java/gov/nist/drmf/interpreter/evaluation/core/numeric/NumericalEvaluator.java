package gov.nist.drmf.interpreter.evaluation.core.numeric;

import com.maplesoft.externalcall.MapleException;
import gov.nist.drmf.interpreter.common.TranslationInformation;
import gov.nist.drmf.interpreter.common.cas.Constraints;
import gov.nist.drmf.interpreter.common.eval.*;
import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import gov.nist.drmf.interpreter.common.exceptions.InitTranslatorException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.eval.NumericResult;
import gov.nist.drmf.interpreter.common.replacements.LogManipulator;
import gov.nist.drmf.interpreter.core.api.DLMFTranslator;
import gov.nist.drmf.interpreter.evaluation.common.Case;
import gov.nist.drmf.interpreter.evaluation.common.CaseAnalyzer;
import gov.nist.drmf.interpreter.evaluation.common.Status;
import gov.nist.drmf.interpreter.evaluation.core.AbstractEvaluator;
import gov.nist.drmf.interpreter.maple.MapleConnector;
import gov.nist.drmf.interpreter.maple.common.MapleConstants;
import gov.nist.drmf.interpreter.mathematica.MathematicaConnector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Andre Greiner-Petter
 */
@SuppressWarnings("ALL")
public class NumericalEvaluator extends AbstractNumericalEvaluator {//implements Observer {

    private static final Logger LOG = LogManager.getLogger(NumericalEvaluator.class.getName());

    // mathematica
//    private static final String SKIP = "5719,5752,9031";

    // maple skips
    private static final String SKIP = ""
            // sections 1 - 9
//            "1652,1653,2126,2363,2474,2679,2717,2917," +
//            // section 10
//            "3061,3062,3351,3352,3353,3370,3371,3372,3435,3491,3501,3516,3586,3587,3588,3616,3618," +
//            // section 11, 12
//            "4005,4093," +
//            // section 13
//            "4440,4482,4483,4484,4485,4486,4487,4488,4489,4521,4522,4524,4603,4609,4610,4613," +
//            // S 14
//            "4736,4850,4857,4858,4864,4866,4867,4868,4869,4870,4871,4872,4873,4874,4898,4918," +
//            // S 15
//            "4971,4973,4984,5008,5092,5097-5116," +
//            // S 16
//            "5166,5211," +
//            // S 18
//            "5482,5607,5609-5612,5706,5782,5835," +
//            // S 19
//            "6277,6278,6610," +
//            // S 25
//            "7668,7672," +
//            // S 28-32
//            "8505,8936,8946,8963,9035,9387,"+
//            "5719,5752,9031"
            ;

    private static final String TESTS = "1257, 1450, 3375, 3376, 3393, 5483, 5767, 5769, 5770, 5771, 5772, 5773, 5774, 5778, 5780, 5784, 5984, 6802, 6803, 9456, 9496, 9566, 9569, 9582, 9647";

    private Set<Integer> realSkips;

    private boolean isMaple = true;

    private static final Pattern nullPattern =
            Pattern.compile("[\\s()\\[\\]{}]*0\\.?0*[\\s()\\[\\]{}]*");

    public static boolean SKIP_SUC_SYMB = true;

    // 0.6GB
    public static final long MEMORY_NOTIFY_LIMIT_KB = 500_000;

    public static final int MAX_LOG_LENGTH = 300;

    private NumericalConfig config = NumericalConfig.config();

    private HashMap<Integer, String> labelLib;

    private LinkedList<String>[] lineResult;

    private int[] subset;

    private HashSet<Integer> testSet;

    private int gcCaller = 0;

    private final INumericalEvaluationScripts scriptHandler;

    private int currentTestCase = 0;
    private int currentNumOfTestCases = 0;

    private HashSet<Integer> lastSkips;

    private List<String> globalConstraints;

    private boolean reserveMODE = false;

    /**
     * Creates an object for numerical evaluations.
     * Workflow:
     * 1) invoke init();
     * 2) loadTestCases();
     * 3) performTests();
     *
     * @throws IOException
     */
    public NumericalEvaluator(
            NativeComputerAlgebraInterfaceBuilder interfaceBuilder
    ) throws ComputerAlgebraSystemEngineException, InitTranslatorException {
        super(new DLMFTranslator(interfaceBuilder.getLanguageKey()), interfaceBuilder.getCASEngine(), interfaceBuilder.getNumericEvaluator());
        this.scriptHandler = interfaceBuilder.getEvaluationScriptHandler();

        // use blueprints to parse constraints
        CaseAnalyzer.ACTIVE_BLUEPRINTS = true;

        this.config = config;
        this.labelLib = new HashMap<>();
        this.lastSkips = new HashSet<>();
//        String[] smr = SKIP_MAPLE_RUNS.split(",");
//        for ( String s : smr ) {
//            int i = Integer.parseInt(s);
//            lastSkips.add(i);
//        }

//        smr = LONG_RUNTIME_SKIP.split(",");
        this.realSkips = new HashSet<>();
        this.testSet = new HashSet<>();
        for ( String s : SKIP.split(",") ) {
            if ( s.isBlank() ) continue;
            if ( s.contains("-") ) {
                String[] tmp = s.split("-");
                int start = Integer.parseInt(tmp[0]);
                int end = Integer.parseInt(tmp[1]);
                for ( int i = start; i <= end; i++ )
                    realSkips.add(i);
            } else {
                int i = Integer.parseInt(s);
                realSkips.add(i);
            }
        }

        for ( String s : TESTS.split(",") ) {
            int i = Integer.parseInt(s.trim());
            testSet.add(i);
        }

        setUpScripts(interfaceBuilder.getNumericProcedures());
        interfaceBuilder.getNumericEvaluator().setTimeout( config.getTimeout() );

        Status.reset();
        init();
    }

    public void init() {
        LOG.info("Setup numerical tests...");
        String overallAss = config.getEntireTestSuiteAssumptions();
        if ( overallAss != null && !overallAss.isEmpty() ){
            String[] ass = overallAss.split(" \\|\\| ");
            String[] transAss = this.getThisConstraintTranslator().translateEachConstraint(ass);
            this.globalConstraints = Arrays.asList(transAss);
            try {
                super.setGlobalNumericAssumptions(globalConstraints);
            } catch (ComputerAlgebraSystemEngineException e) {
                LOG.error("Unable to enter global assumptions", e);
            }
        }
    }

    @Override
    public LinkedList<Case> loadTestCases() {
        subset = config.getSubSetInterval();
        HashMap<Integer, String> skippedLinesInfo = new HashMap<>();

        Set<ID> skips = null;
        if ( reserveMODE )
            skips = getSpecificResults(config.getSymbolicResultsPath(), ".*Successful.*");
        else skips = getSpecificResults(config.getSymbolicResultsPath(), ".*(Failure|Aborted).*");

        if ( skips != null && config.getSymbolicResultsPath() != null ) {
            LOG.info("Symbolic results are specified. Ignore specified subset and test for values in the result data.");
            ID min = skips.stream().min( (u, v) -> u.id.compareTo(v.id)).get();
            ID max = skips.stream().max( (u, v) -> u.id.compareTo(v.id)).get();
            subset = new int[]{min.id, max.id + 1};
        }

        boolean reverse = true;

        if ( config.getSymbolicResultsPath() == null ) reverse = false;

        LinkedList<Case> testCases = loadTestCases(
                subset,
                skips,
                config.getDataset(),
                labelLib,
                skippedLinesInfo,
                reverse
        );

        lineResult = new LinkedList[subset[1]];
        for ( Integer i : skippedLinesInfo.keySet() ){
            lineResult[i] = new LinkedList<>();
            lineResult[i].add(skippedLinesInfo.get(i));
        }

        currentNumOfTestCases = testCases.size();
        currentTestCase = 0;
        return testCases;
    }

    @Override
    public int[] getResultInterval() {
        return subset;
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
    public void performSingleTest( Case c ){
//        if ( !testSet.contains(c.getLine()) ) {
//            // just ignore that shit
//            return;
//        }

        if ( lineResult[c.getLine()] == null ){
            lineResult[c.getLine()] = new LinkedList();
        }

        LOG.info("Start test for line: " + c.getLine());
        LOG.info("Test case: " + c);
        LOG.info("Test case " + (currentTestCase++) + " of " + currentNumOfTestCases);

        LOG.info("Replacing defined symbols.");
        c.replaceSymbolsUsed(super.getSymbolDefinitionLibrary());
        LOG.info("Final Test case: " + c);

        if ( realSkips.contains(c.getLine()) ) {
            LOG.warn("Skip this test case manually for reasons...");
            Status.SKIPPED.add();
            lineResult[c.getLine()].add("Manual Skip!");
            return;
        }

//        if ( isMaple ) {
//            String s = c.getLHS() + " " + c.getRHS();
//            if ( s.matches(".*\\\\(?:int|sum|prod|lim).*") ) {
//                LOG.info("Skip bullshit int etc...");
//                lineResult[c.getLine()].add("Skip - int/sum/prod/lim");
//                Status.MISSING.add();
//                return;
//            }
//        }

//        if ( isMaple && lastSkips.contains(c.getLine()) ) {
//            LOG.info("Final Skip " + c.getLine());
//            lineResult[c.getLine()].add("Skip - too long running...");
//            Status.MISSING.add();
//            return;
//        }

        if ( c instanceof AbstractEvaluator.DummyCase) {
            lineResult[c.getLine()].add("Skip - symbolical successful subtest");
            Status.SKIPPED.add();
            return;
        }

//        String expression = null;
        Constraints con = null;
        try {
            Status.STARTED_TEST_CASES.add();

            con = c.getConstraintObject();
            if (con != null) {
                if (!con.specialValuesInfo().isEmpty()) {
                    LOG.info(con.specialValuesInfo());
                }
                LOG.info(con.constraintInfo());
            }
        } catch ( Error | Exception te ) {
            LOG.error("Error in translation of a constraint: " + te.toString());
        }

        String[] preAndPostCommands = null;
        try {
            preAndPostCommands = getPrevCommand( c.getLHS() + ", " + c.getRHS() );
            if ( preAndPostCommands[0] != null ){
                LOG.debug("Enter pre-testing commands: " + preAndPostCommands[0]);
                enterEngineCommand(preAndPostCommands[0]);
            }
        } catch (Error | Exception e) {
            // nothing.
            LOG.warn("Unable to enter pre-testing commands: " + c);
        }

        NumericalTest test = null;
        try {
            LOG.debug("Start numerical calculations.");
            test = buildTestObject(c);
            Status.SUCCESS_TRANS.add();
            LOG.info("Numerical test expression: " + test.getTestExpression());
        } catch ( TranslationException te ) {
            LOG.error("Error in translation. " + te.toString());
            lineResult[c.getLine()].add("Error - " + te.toString());
            Status.ERROR_TRANS.add();
            return;
        } catch ( NullPointerException npe ) {
            LOG.error("Unable to analyze test case properly: " + c);
            lineResult[c.getLine()].add("Error - Invalid Test case: " + c);
            Status.ERROR_TRANS.add();
        }

        if ( test == null ) return;

        try {
            if ( isMaple ) test.setSkipClassicAbortion();
            NumericResult results = performNumericalTest(test);

            LOG.debug("Finished numerical calculations.");
            if ( preAndPostCommands[1] != null ){
                enterEngineCommand(preAndPostCommands[1]);
                LOG.debug("Enter post-testing commands: " + preAndPostCommands[1]);
            }

            boolean wasAborted = isAbortedResult(results);
            if ( wasAborted ) {
                LOG.warn("Skip test because it took too much time.");
                lineResult[c.getLine()].add("Skipped - Because timed out");
                Status.ABORTED.add();
            } else {
                TestResultType resType = testResult(results);
                String evaluation = "";
                int tested = results.getNumberOfTotalTests();
                int failed = results.getNumberOfFailedTests();
                if ( tested == 0 && failed > 0 ) {
                    lineResult[c.getLine()].add("Skip - No test values generated");
                    Status.NO_TEST_VALUES.add();
                } else {
                    switch (resType) {
                        case SUCCESS:
                            lineResult[c.getLine()].add("Successful [Tested: " + tested + "]");
                            Status.SUCCESS_NUM.add();
                            break;
                        case FAILURE:
                            LOG.info("Test was NOT successful.");
                            evaluation = LogManipulator.shortenOutput(results.toString(), 2);
                            lineResult[c.getLine()].add("Failed ["+failed+"/"+tested+"]: " + evaluation);
                            Status.FAILURE.add();
                            break;
                        case ERROR:
                            LOG.info("Test was NOT successful.");
                            evaluation = LogManipulator.shortenOutput(results.toString(), 2);
                            lineResult[c.getLine()].add("Error [" + evaluation + "]");
                            Status.ERROR.add();
                            break;
                    }
                }
            }

            LOG.info("Finished test for line: " + c.getLine());
        } catch ( IllegalArgumentException iae ) {
            LOG.warn("Skip test, because " + iae.getMessage());
            lineResult[c.getLine()].add("Error - " + iae.getMessage());
            // Note, we rename the overview lines, so we use missing here, just to avoid trouble with SKIP infos
            Status.ERROR.add();
        } catch ( Error | Exception e ){
            LOG.warn("Error for line " + c.getLine() + ", because: " + e.toString(), e);
            lineResult[c.getLine()].add("Error - " + e.toString());
            Status.ERROR.add();
        } finally {
            // garbage collection
            try { forceGC(); }
            catch ( ComputerAlgebraSystemEngineException me ){
                LOG.fatal("Cannot call Maple's garbage collector!", me);
            }
        }
//        return c.getLine() + ": " + lineResult[c.getLine()];
    }

    private NumericalTest buildTestObject(Case c) {
        super.startRememberPackages();
        LOG.debug("Translating LHS: " + c.getLHS());
        TranslationInformation lhs = forwardTranslate( c.getLHS(), c.getEquationLabel() );
        LOG.info("Translated LHS to: " + lhs.getTranslatedExpression());

        LOG.debug("Translating RHS: " + c.getRHS());
        TranslationInformation rhs = forwardTranslate( c.getRHS(), c.getEquationLabel() );
        LOG.info("Translated RHS to: " + rhs.getTranslatedExpression());
        super.stopRememberPackages();

        Set<String> variables = new HashSet<>();
        variables.addAll(lhs.getFreeVariables().getFreeVariables());
        variables.addAll(rhs.getFreeVariables().getFreeVariables());

        NumericalTest test = new NumericalTest(
                getTestedExpression(lhs.getTranslatedExpression(), rhs.getTranslatedExpression(), c),
                c,
                config,
                getThisConstraintTranslator()
        );
        test.setPostProcessingMethodName(scriptHandler.getPostProcessingScriptName(c.isEquation()));
        test.setVariables(variables);
        return test;
    }

    private String getTestedExpression(String lhs, String rhs, Case c){
        if ( !c.isEquation() ){
            return lhs + c.getRelation().getSymbol() + rhs;
        }

        Matcher nullLHSMatcher = nullPattern.matcher( lhs );
        Matcher nullRHSMatcher = nullPattern.matcher( rhs );
        if ( nullLHSMatcher.matches() ) {
            lhs = "";
        }
        if ( nullRHSMatcher.matches() ) {
            rhs = "";
        }

        return config.getTestExpression(
                this.getNumericEvaluator()::generateNumericTestExpression,
                lhs, rhs
        );
    }

    @Override
    public LinkedList<String>[] getLineResults(){
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
     * @return array of length 2 (0: previous commands, 1: after test commands)
     */
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

    private boolean requestedRestart = false;
    private int factor = 1;

    public static NumericalEvaluator createStandardMapleEvaluator()
            throws IOException, MapleException, ComputerAlgebraSystemEngineException, InitTranslatorException {
        NumericalEvaluator evaluator = new NumericalEvaluator(new MapleConnector());
        evaluator.isMaple = true;
        return evaluator;
    }

    public static NumericalEvaluator createStandardMathematicaEvaluator() throws IOException, ComputerAlgebraSystemEngineException, InitTranslatorException {
        NumericalEvaluator evaluator = new NumericalEvaluator(new MathematicaConnector());
        evaluator.isMaple = false;
        return evaluator;
    }

    private static void startTestAndWriteResults( NumericalEvaluator evaluator ) throws IOException {
        LinkedList<Case> tests = evaluator.loadTestCases();
        evaluator.performAllTests(tests);
        evaluator.writeResults();
    }

    public static void main(String[] args) throws Exception{

        NumericalEvaluator evaluator = null;
        if ( args == null || args.length < 1 ){
            System.out.println("Start Mathematica Evaluator");
            evaluator = createStandardMathematicaEvaluator();
        }

        boolean successful = false;
        if ( args != null ) {
            for ( String arg : args ) {
                if ( arg.matches("--?mathematica") ) {
                    System.out.println("Start Mathematica Evaluator");
                    evaluator = createStandardMathematicaEvaluator();
                } else if ( arg.matches("--?maple") ) {
                    System.out.println("Start Maple Evaluator");
                    evaluator = createStandardMapleEvaluator();
                } else if ( arg.matches("--?(reverse|success|successful)") ) {
                    System.out.println("Successful mode");
                    successful = true;
                }
            }
        }

        evaluator.reserveMODE = successful;
        evaluator.startTestAndWriteResults(evaluator);
    }

    protected int getGcCaller() {
        return gcCaller;
    }

    protected void stepGcCaller() {
        this.gcCaller++;
    }

    protected void resetGcCaller() {
        this.gcCaller = 0;
    }
}
