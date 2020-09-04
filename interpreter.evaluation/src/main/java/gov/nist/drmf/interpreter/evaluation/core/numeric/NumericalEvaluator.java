package gov.nist.drmf.interpreter.evaluation.core.numeric;

import com.maplesoft.externalcall.MapleException;
import com.maplesoft.openmaple.Algebraic;
import com.wolfram.jlink.Expr;
import gov.nist.drmf.interpreter.cas.constraints.Constraints;
import gov.nist.drmf.interpreter.cas.constraints.IConstraintTranslator;
import gov.nist.drmf.interpreter.common.cas.ICASEngineNumericalEvaluator;
import gov.nist.drmf.interpreter.common.cas.IComputerAlgebraSystemEngine;
import gov.nist.drmf.interpreter.common.constants.GlobalPaths;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import gov.nist.drmf.interpreter.common.exceptions.InitTranslatorException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.core.DLMFTranslator;
import gov.nist.drmf.interpreter.evaluation.common.Case;
import gov.nist.drmf.interpreter.evaluation.common.CaseAnalyzer;
import gov.nist.drmf.interpreter.evaluation.common.ProcedureLoader;
import gov.nist.drmf.interpreter.evaluation.common.Status;
import gov.nist.drmf.interpreter.evaluation.core.AbstractEvaluator;
import gov.nist.drmf.interpreter.evaluation.core.EvaluationConfig;
import gov.nist.drmf.interpreter.evaluation.core.symbolic.SymbolicEvaluator;
import gov.nist.drmf.interpreter.maple.common.MapleConstants;
import gov.nist.drmf.interpreter.maple.extension.MapleInterface;
import gov.nist.drmf.interpreter.maple.extension.NumericCalculator;
import gov.nist.drmf.interpreter.maple.translation.MapleTranslator;
import gov.nist.drmf.interpreter.mathematica.extension.MathematicaInterface;
import gov.nist.drmf.interpreter.mathematica.extension.MathematicaNumericalCalculator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Andre Greiner-Petter
 */
@SuppressWarnings("ALL")
public class NumericalEvaluator<T> extends AbstractNumericalEvaluator<T> {//implements Observer {

    private static final Logger LOG = LogManager.getLogger(NumericalEvaluator.class.getName());

//    protected static final String LONG_RUNTIME_SKIP = "89,90,91,99,100,102";
    //TODO MATHEMATICA SKIPS
    public static final String LONG_RUNTIME_SKIP =
        "103,275,402,640,649,1248,1315,1316,1317,1318,1319,1320,1321,1322,1323,1324,1325,1326,1410," +
                "1445,1460,1461,1462,1463,1464,1465,1466,1467,1468,1469,1470,1471,1542," +
                "2068,2498,2562,2563,2564,2565,2566,2871," +
                "3035,3033,3035,3224,3358,3437,3494,3816,3817,3983," +
                "4212,4213,4214,4280,4343,4439,4440,4464,4485,4486,4550,4551,4591,4592,4593,4608,4609,4818,4831,4832,4911,4964," +
                "5061,5062,5063,5064," +
                "5814,5815,5933,5992,5993,5994,5995,6017,6957," +
                "6325,6349,6370,6536,6907," +
                "7313,7330,7331,7332,7333,7336,7339,7394,7397,7398,7399,7401,7405,7902,7918,7925,5828,5935," +
                "9572";

//    public static final String SKIP_MAPLE_RUNS =
//            "321,642,794,795,850,976,1266,1267,1268,1269,1516,1948,1949," +
//                    "2025,2058,2062,2067,2069,2070,2071,2072,2073,2074,2076,2100,2117,2118,2119,2120," +
//                    "2268,2345,2351,2352,2362,2366,2406,2485,2487,2488,2491,2493,2494,2495,2496,2517," +
//                    "2518,2519,2521,2617,2618,2619," +
//                    "4278,4279,4280,4285,4308,4309,4310,4311,4338,4391," +
//                    "4738,4755,4811,4812,4813," +
//                    "5214,5224,5226,5252,5375," +
//                    "5866,5867,5868,5869,5870,5871,5872," +
//                    "6430,6440," +
//                    "9218,9219," +
//                    "9349";

    private Set<Integer> realSkips;

    private boolean isMaple = true;

    private static final Pattern nullPattern =
            Pattern.compile("[\\s()\\[\\]{}]*0\\.?0*[\\s()\\[\\]{}]*");

    public static boolean SKIP_SUC_SYMB = true;

    // 0.6GB
    public static final long MEMORY_NOTIFY_LIMIT_KB = 500_000;

    public static final int MAX_LOG_LENGTH = 300;

    private static Path output;

    private NumericalConfig config;

    private HashMap<Integer, String> labelLib;

    private LinkedList<String>[] lineResult;

    private int[] subset;

    private int gcCaller = 0;

    private final INumericalEvaluationScripts scriptHandler;

    private int currentTestCase = 0;
    private int currentNumOfTestCases = 0;

    private HashSet<Integer> lastSkips;

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
            IConstraintTranslator forwardTranslator,
            IComputerAlgebraSystemEngine<T> engine,
            ICASEngineNumericalEvaluator<T> numericalEvaluator,
            INumericalEvaluationScripts scriptHandler,
            String[] defaultPrevAfterCmds,
            String[] procedures,
            NumericalConfig config
    ) throws ComputerAlgebraSystemEngineException {
        super(forwardTranslator, engine, numericalEvaluator);
        this.scriptHandler = scriptHandler;

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
//        for ( String s : smr ) {
//            int i = Integer.parseInt(s);
//            realSkips.add(i);
//        }

        setUpScripts(procedures);

        Status.reset();
    }

    public void init() throws IOException, MapleException {
        LOG.info("Setup numerical tests...");
        output = config.getOutputPath();
        if (!Files.exists(output)) {
            Files.createFile(output);
        }
    }

    @Override
    public LinkedList<Case> loadTestCases() {
        subset = config.getSubSetInterval();
        HashMap<Integer, String> skippedLinesInfo = new HashMap<>();

        Set<ID> skips = getSpecificResults(config.getSymbolicResultsPath(), "Failure");
//        Set<ID> skips = getSpecificResults(config.getSymbolicResultsPath(), "Successful");

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
        LOG.info("Start test for line: " + c.getLine());
        LOG.info("Test case: " + c);
        LOG.info("Test case " + (currentTestCase++) + " of " + currentNumOfTestCases);


        if ( lineResult[c.getLine()] == null ){
            lineResult[c.getLine()] = new LinkedList();
        }

        if ( realSkips.contains(c.getLine()) ) {
            LOG.warn("Skip, it take ages...");
            Status.SKIPPED.add();
            lineResult[c.getLine()].add("Manual skip because it never finishes!");
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

        if ( isMaple && lastSkips.contains(c.getLine()) ) {
            LOG.info("Final Skip " + c.getLine());
            lineResult[c.getLine()].add("Skip - too long running...");
            Status.MISSING.add();
            return;
        }

        if ( c instanceof AbstractEvaluator.DummyCase) {
            lineResult[c.getLine()].add("Skip - symbolical successful subtest");
            Status.SKIPPED.add();
            return;
        }

        try {
            Constraints con =  c.getConstraintObject();
            if ( con != null ){
                if ( !con.specialValuesInfo().isEmpty() )
                    LOG.info(con.specialValuesInfo());
                LOG.info(con.constraintInfo());
            }

            String expression = getTestedExpression(c);
            Status.SUCCESS_TRANS.add();
            LOG.info("Numerical test expression: " + expression);

            String[] preAndPostCommands = getPrevCommand( c.getLHS() + ", " + c.getRHS() );

            if ( preAndPostCommands[0] != null ){
                LOG.debug("Enter pre-testing commands: " + preAndPostCommands[0]);
                enterEngineCommand(preAndPostCommands[0]);
            }

            LOG.debug("Start numerical calculations.");
            NumericalTest test = buildTestObject(expression, c);
            if ( isMaple ) test.setSkipClassicAbortion();
            T results = performNumericalTest(test);

            LOG.debug("Finished numerical calculations.");
            if ( preAndPostCommands[1] != null ){
                enterEngineCommand(preAndPostCommands[1]);
                LOG.debug("Enter post-testing commands: " + preAndPostCommands[1]);
            }

            boolean wasAborted = isAbortedResult(results);
            if ( wasAborted ) {
                LOG.warn("Skip test because it took too much time.");
                lineResult[c.getLine()].add("Skipped - Because timed out");
                Status.SKIPPED.add();
            } else {
                ICASEngineNumericalEvaluator.ResultType resType = testResult(results);
                String evaluation = "";
                switch (resType) {
                    case SUCCESS:
                        lineResult[c.getLine()].add("Successful");
                        Status.SUCCESS_NUM.add();
                        break;
                    case FAILURE:
                        LOG.info("Test was NOT successful.");
                        evaluation = shortenOutput(results.toString());
                        lineResult[c.getLine()].add(evaluation);
                        Status.FAILURE.add();
                        break;
                    case ERROR:
                        LOG.info("Test was NOT successful.");
                        evaluation = shortenOutput(results.toString());
                        lineResult[c.getLine()].add(evaluation);
                        Status.ERROR.add();
                        break;
                }
            }


            LOG.info("Finished test for line: " + c.getLine());
        } catch ( TranslationException te ) {
            LOG.error("Error in translation. " + te.toString());
            lineResult[c.getLine()].add("Error - " + te.toString());
            Status.ERROR.add();
        } catch ( IllegalArgumentException iae ){
            LOG.warn("Skip test, because " + iae.getMessage());
            lineResult[c.getLine()].add("Skipped - " + iae.getMessage());
            // Note, we rename the overview lines, so we use missing here, just to avoid trouble with SKIP infos
            Status.MISSING.add();
        } catch ( Exception e ){
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

    private NumericalTest buildTestObject(String expression, Case c) {
        NumericalTest test = new NumericalTest(expression, c, config, getThisConstraintTranslator());
        test.setPostProcessingMethodName(scriptHandler.getPostProcessingScriptName(c));
        return test;
    }

    private String getTestedExpression(Case c){
        LOG.debug("Translating LHS: " + c.getLHS());
        String mapleLHS = forwardTranslate( c.getLHS(), c.getEquationLabel() );
        LOG.info("Translated LHS to: " + mapleLHS);

        LOG.debug("Translating RHS: " + c.getRHS());
        String mapleRHS = forwardTranslate( c.getRHS(), c.getEquationLabel() );
        LOG.info("Translated RHS to: " + mapleRHS);

        if ( !c.isEquation() ){
            return mapleLHS + c.getRelation().getSymbol() + mapleRHS;
        }

        Matcher nullLHSMatcher = nullPattern.matcher( mapleLHS );
        Matcher nullRHSMatcher = nullPattern.matcher( mapleRHS );
        if ( nullLHSMatcher.matches() ) {
            mapleLHS = "";
        }
        if ( nullRHSMatcher.matches() ) {
            mapleRHS = "";
        }

        return config.getTestExpression( this.getNumericalEvaluator(), mapleLHS, mapleRHS );
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
        String[] mapleScripts = new String[3];
        String numericalProc = MapleTranslator.extractProcedure(GlobalPaths.PATH_MAPLE_NUMERICAL_PROCEDURES);
        mapleScripts[0] = numericalProc;

        // load expectation of results template
        NumericalConfig config =  NumericalConfig.config();
        String expectationTemplate = config.getExpectationTemplate();
        // load numerical sieve
        String sieve_procedure = MapleTranslator.extractProcedure( GlobalPaths.PATH_MAPLE_NUMERICAL_SIEVE_PROCEDURE );
        String sieve_procedure_relation = "rel" + sieve_procedure;

        // replace condition placeholder
        String numericalSievesMethod = MapleTranslator.extractNameOfProcedure(sieve_procedure);
        String numericalSievesMethodRelations = "rel" + numericalSievesMethod;

        sieve_procedure = sieve_procedure.replaceAll(
                NumericalTestConstants.KEY_NUMERICAL_SIEVES_CONDITION,
                expectationTemplate
        );

        sieve_procedure_relation = sieve_procedure_relation.replaceAll(
                NumericalTestConstants.KEY_NUMERICAL_SIEVES_CONDITION,
                "result"
        );

        mapleScripts[1] = sieve_procedure;
        mapleScripts[2] = sieve_procedure_relation;
        LOG.debug("Setup done!");

        DLMFTranslator dlmfTranslator = new DLMFTranslator(Keys.KEY_MAPLE);
        MapleInterface mapleInterface = MapleInterface.getUniqueMapleInterface();
        NumericCalculator numericCalculator = new NumericCalculator();

        numericCalculator.setTimeLimit(2);

        NumericalEvaluator evaluator = new NumericalEvaluator<Algebraic>(
                dlmfTranslator,
                mapleInterface,
                numericCalculator,
                (c -> c.isEquation() ? numericalSievesMethod : numericalSievesMethodRelations),
                SymbolicEvaluator.getMaplePrevAfterCommands(),
                mapleScripts,
                config
        );

        evaluator.isMaple = true;

        return evaluator;
    }

    public static NumericalEvaluator createStandardMathematicaEvaluator() throws IOException, ComputerAlgebraSystemEngineException, InitTranslatorException {
        NumericalConfig config =  NumericalConfig.config();

        DLMFTranslator dlmfTranslator = new DLMFTranslator(Keys.KEY_MATHEMATICA);
        MathematicaInterface mathematicaInterface = MathematicaInterface.getInstance();
        MathematicaNumericalCalculator numericalCalculator = new MathematicaNumericalCalculator();

        String script = ProcedureLoader.getProcedure(GlobalPaths.PATH_MATHEMATICA_NUMERICAL_PROCEDURES);

        NumericalEvaluator evaluator = new NumericalEvaluator<Expr>(
                dlmfTranslator,
                mathematicaInterface,
                numericalCalculator,
                (c -> c.isEquation() ? "" : ""),
                null,
                new String[]{script},
                config
        );

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
