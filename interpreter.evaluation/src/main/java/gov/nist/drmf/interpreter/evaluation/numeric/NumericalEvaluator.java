package gov.nist.drmf.interpreter.evaluation.numeric;

import com.maplesoft.externalcall.MapleException;
import com.maplesoft.openmaple.Algebraic;
import com.wolfram.jlink.Expr;
import gov.nist.drmf.interpreter.MapleSimplifier;
import gov.nist.drmf.interpreter.MapleTranslator;
import gov.nist.drmf.interpreter.MathematicaTranslator;
import gov.nist.drmf.interpreter.common.Case;
import gov.nist.drmf.interpreter.common.CaseAnalyzer;
import gov.nist.drmf.interpreter.common.Status;
import gov.nist.drmf.interpreter.common.Util;
import gov.nist.drmf.interpreter.common.constants.GlobalPaths;
import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.grammar.IComputerAlgebraSystemEngine;
import gov.nist.drmf.interpreter.constraints.Constraints;
import gov.nist.drmf.interpreter.constraints.IConstraintTranslator;
import gov.nist.drmf.interpreter.evaluation.*;
import gov.nist.drmf.interpreter.evaluation.symbolic.SymbolicEvaluator;
import gov.nist.drmf.interpreter.maple.common.MapleConstants;
import gov.nist.drmf.interpreter.maple.translation.MapleInterface;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
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
    protected static final String LONG_RUNTIME_SKIP =
        "103,275,402,640,649,1248,1315,1316,1317,1318,1319,1320,1321,1322,1323,1324,1325,1326,1410," +
                "1445,1460,1461,1462,1463,1464,1465,1466,1467,1468,1469,1470,1471,1542," +
                "2068,2498,2562,2563,2564,2565,2566,2871," +
                "3035,3033,3035,3224,3358,3437,3494,3816,3817,3983," +
                "4212,4213,4214,4280,4343,4439,4440,4464,4485,4486,4550,4551,4591,4592,4593,4608,4609,4818,4831,4832,4911,4964," +
                "5061,5062,5063,5064," +
                "5814,5815,5933,5992,5993,5994,5995,6017,6957," +
                "6325,6349,6370,6536,6907," +
                "7313,7330,7331,7332,7333,7336,7339,7394,7397,7398,7399,7401,7405," +
                "9572";

    protected static final String SKIP_MAPLE_RUNS =
            "321,642,794,795,850,976,1266,1267,1268,1269,1516,1948,1949," +
                    "2025,2058,2062,2067,2069,2070,2071,2072,2073,2074,2076,2100,2117,2118,2119,2120," +
                    "2268,2345,2351,2352,2362,2366,2406,2485,2487,2488,2491,2493,2494,2495,2496,2517," +
                    "2518,2519,2521,2617,2618,2619," +
                    "4278,4279,4280,4285,4308,4309,4310,4311,4338,4391," +
                    "4738,4755,4811,4812,4813," +
                    "5214,5224,5226,5252,5375," +
                    "5866,5867,5868,5869,5870,5871,5872," +
                    "6430,6440," +
                    "9218,9219," +
                    "9349";

    public static final int[] POT_DIFF = new int[]{
            54, 55, 55, 83, 88, 95, 96, 100, 101, 104, 105, 106, 238, 239, 240, 241, 246, 251, 256, 260, 440, 445, 455,
            456, 523, 546, 549, 595, 616, 625, 626, 636, 645, 646, 655, 656, 658, 675, 677, 720, 780, 791, 850, 890, 891,
            894, 902, 909, 913, 918, 932, 955, 958, 1234, 1251, 1337, 1349, 1352, 1487, 1488, 1494, 1496, 1498, 1504, 1515,
            1516, 1558, 1560, 1561, 1562, 1563, 1564, 1565, 1567, 1570, 1571, 1572, 1593, 1602, 1605, 1607, 1611, 1612, 1612,
            1619, 1620, 1665, 1666, 1667, 1669, 1691, 1691, 1692, 1693, 1693, 1706, 1707, 1709, 1710, 1711, 1712, 1713, 1714,
            1715, 1721, 1723, 1724, 1726, 1738, 1738, 1740, 1741, 1741, 1744, 1745, 1745, 1747, 1748, 1749, 1750, 1751, 1751,
            1753, 1753, 1756, 1772, 1772, 1773, 1773, 1789, 1801, 1802, 1805, 1835, 1836, 1837, 1838, 1840, 1842, 1862, 1864,
            1864, 1876, 1877, 1878, 1879, 1880, 1881, 1882, 1888, 1891, 1898, 1898, 1901, 1901, 1904, 1905, 1905, 1906, 1906,
            1907, 1909, 1910, 1910, 1912, 1913, 1914, 1915, 1916, 1916, 1918, 1918, 1928, 1928, 1930, 1930, 1956, 1957, 1957,
            1958, 1958, 1959, 1959, 1960, 1960, 1961, 1994, 2025, 2033, 2152, 2155, 2171, 2172, 2173, 2173, 2179, 2180, 2181,
            2184, 2184, 2189, 2203, 2205, 2205, 2206, 2206, 2209, 2209, 2210, 2211, 2212, 2213, 2214, 2216, 2217, 2218, 2254,
            2255, 2271, 2271, 2273, 2282, 2341, 2350, 2371, 2372, 2373, 2375, 2375, 2392, 2396, 2400, 2402, 2411, 2412, 2420,
            2422, 2422, 2423, 2424, 2425, 2427, 2428, 2429, 2430, 2431, 2449, 2450, 2458, 2459, 2460, 2463, 2465, 2468, 2468,
            2470, 2478, 2479, 2526, 2597, 2597, 2601, 2614, 2616, 2619, 2638, 2662, 2663, 2679, 2682, 2748, 2752, 2752, 2753,
            2753, 2754, 2754, 2755, 2755, 2756, 2757, 2758, 2759, 2761, 2761, 2762, 2762, 2763, 2763, 2764, 2764, 2765, 2765,
            2766, 2766, 2767, 2768, 2769, 2770, 2772, 2772, 2773, 2774, 2775, 2776, 2811, 2812, 2813, 2814, 2823, 2853, 2867,
            2878, 2879, 2881, 2895, 2897, 2902, 2918, 2945, 2975, 3036, 3036, 3055, 3063, 3073, 3074, 3075, 3076, 3077, 3078,
            3080, 3081, 3137, 3137, 3202, 3203, 3204, 3205, 3206, 3207, 3208, 3209, 3210, 3246, 3246, 3325, 3356, 3357, 3385,
            3385, 3401, 3430, 3434, 3450, 3454, 3455, 3457, 3458, 3461, 3462, 3470, 3470, 3471, 3471, 3473, 3474, 3475, 3475,
            3476, 3477, 3506, 3517, 3518, 3519, 3519, 3520, 3520, 3521, 3522, 3522, 3523, 3524, 3547, 3898, 3898, 3903, 3905,
            3949, 4068, 4069, 4070, 4071, 4078, 4078, 4083, 4083, 4084, 4084, 4085, 4085, 4096, 4105, 4110, 4112, 4113, 4114,
            4115, 4118, 4118, 4119, 4120, 4121, 4122, 4137, 4137, 4245, 4272, 4344, 4379, 4382, 4383, 4384, 4389, 4403, 4487,
            4487, 4673, 4674, 4704, 4705, 4711, 4711, 4712, 4712, 4713, 4714, 4715, 4716, 4717, 4718, 4719, 4720, 4721, 4722,
            4724, 4725, 4730, 4731, 4732, 4733, 4734, 4734, 4735, 4762, 4762, 4767, 4771, 4777, 4794, 4795, 4809, 4825, 4847,
            4850, 4862, 4877, 4902, 4916, 4936, 4939, 4940, 4944, 5023, 5082, 5083, 5086, 5110, 5207, 5224, 5672, 5695, 5696,
            5826, 5838, 5852, 5855, 5991, 5998, 6081, 6082, 6086, 6246, 6328, 6340, 6356, 6357, 6360, 6361, 6362, 6364, 6369,
            6372, 6375, 6380, 6380, 6382, 6387, 6388, 6393, 6410, 6411, 6420, 6422, 6436, 6438, 6439, 6458, 6459, 6466, 6472,
            6473, 6479, 6499, 6500, 6512, 6518, 6519, 6520, 6524, 6525, 6526, 6527, 6528, 6529, 6530, 6531, 6533, 6535, 6537,
            6545, 6547, 6569, 6769, 6945, 6949, 6955, 6956, 6958, 6972, 7034, 7104, 7105, 7112, 7113, 7118, 7212, 7222, 7225,
            7225, 7226, 7226, 7228, 7229, 7230, 7231, 7232, 7234, 7235, 7237, 7238, 7242, 7242, 7243, 7277, 7277, 7279, 7279,
            7280, 7287, 7304, 7305, 7306, 7307, 7308, 7309, 7310, 7311, 7312, 7327, 7342, 7344, 7345, 7347, 7348, 7350, 7356,
            7357, 7359, 7360, 7362, 7366, 7377, 7382, 7383, 7385, 7386, 7417, 7419, 7420, 7441, 7442, 7456, 7457, 7458, 7463,
            7466, 7478, 7479, 7530, 7530, 7535, 7536, 7536, 7626, 7685, 7685, 7686, 7686, 7695, 7701, 7735, 7738, 7739, 7741,
            7742, 7746, 7748, 7773, 7774, 7777, 7783, 7784, 7785, 7787, 7788, 7801, 7860, 7878, 7886, 7890, 7909, 7910, 7912,
            7923, 7945, 7957, 7965, 7967, 7988, 8009, 8023, 8026, 8028, 8037, 8065, 8071, 8082, 8119, 8120, 8120, 8122, 8123,
            8124, 8125, 8126, 8129, 8139, 8142, 8143, 8144, 8147, 8149, 8152, 8153, 8155, 8156, 8157, 8161, 8167, 8249, 8280,
            8388, 8460, 8463, 8476, 8477, 8478, 8479, 8480, 8481, 8482, 8518, 8552, 8569, 8604, 8605, 8722, 8747, 8786, 8790,
            8796, 8855, 8856, 8857, 9014, 9017, 9103, 9113, 9127, 9129, 9224, 9224, 9225, 9225, 9226, 9226, 9230, 9231, 9232,
            9248, 9293, 9490, 9491, 9507, 9832, 9880, 9880, 9881, 9881, 9882, 9882, 9942, 9945, 9961, 10032, 10033, 10034,
            10035, 10318, 10319, 10320, 10321, 10321, 10322, 10322, 10444
    };

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
        String[] smr = SKIP_MAPLE_RUNS.split(",");
        for ( String s : smr ) {
            int i = Integer.parseInt(s);
            lastSkips.add(i);
        }

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

//        Set<ID> skips = getSpecificResults(config.getSymbolicResultsPath(), "Failure");
        Set<ID> skips = getSpecificResults(config.getSymbolicResultsPath(), "Successful");

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

        if ( c.getLine() == 3380 || c.getLine() == 5128 || c.getLine() == 5945 ) {
            LOG.warn("Skip, it take ages...");
            Status.SKIPPED.add();
            lineResult[c.getLine()].add("Manual skip because it never finishes!");
            return;
        }

        if ( isMaple ) {
            String s = c.getLHS() + " " + c.getRHS();
            if ( s.matches(".*\\\\(?:int|sum|prod|lim).*") ) {
                LOG.info("Skip bullshit int etc...");
                lineResult[c.getLine()].add("Skip - int/sum/prod/lim");
                Status.MISSING.add();
                return;
            }
        }

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
            String label = c.getEquationLabel();
            T results = performNumericalTest(
                    expression,
                    config.getListOfNumericalValues(getThisConstraintTranslator(), label),
                    c.getConstraints(getThisConstraintTranslator(), label),
                    c.getConstraintVariables(getThisConstraintTranslator(), label),
                    c.getConstraintValues(),
                    config.getListOfSpecialVariables(getThisConstraintTranslator(), label),
                    config.getListOfSpecialVariableValues(getThisConstraintTranslator(), label),
                    scriptHandler.getPostProcessingScriptName(c),
                    config.getPrecision(),
                    config.getMaximumNumberOfCombs()
            );

            LOG.debug("Finished numerical calculations.");
            if ( preAndPostCommands[1] != null ){
                enterEngineCommand(preAndPostCommands[1]);
                LOG.debug("Enter post-testing commands: " + preAndPostCommands[1]);
            }

            boolean wasAborted = isAbortedResult(results);
            if ( wasAborted ) {
                LOG.info("Skip test because it took too much time.");
                lineResult[c.getLine()].add("Skipped - Because timed out");
                Status.FAILURE.add();
            } else {
                ICASEngineNumericalEvaluator.ResultType resType = testResult(results);
                String evaluation = "";
                switch (resType) {
                    case SUCCESS:
                        lineResult[c.getLine()].add("Successful");
                        Status.SUCCESS.add();
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
            throws IOException, MapleException, ComputerAlgebraSystemEngineException {
        String[] mapleScripts = new String[3];
        String numericalProc = MapleInterface.extractProcedure(GlobalPaths.PATH_MAPLE_NUMERICAL_PROCEDURES);
        mapleScripts[0] = numericalProc;

        // load expectation of results template
        NumericalConfig config =  NumericalConfig.config();
        String expectationTemplate = config.getExpectationTemplate();
        // load numerical sieve
        String sieve_procedure = MapleInterface.extractProcedure( GlobalPaths.PATH_MAPLE_NUMERICAL_SIEVE_PROCEDURE );
        String sieve_procedure_relation = "rel" + sieve_procedure;

        // replace condition placeholder
        String numericalSievesMethod = MapleInterface.extractNameOfProcedure(sieve_procedure);
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

        MapleTranslator translator = new MapleTranslator();
        translator.init();

        MapleSimplifier simplifier = translator.getMapleSimplifier();
        NumericalEvaluator evaluator = new NumericalEvaluator<Algebraic>(
                translator,
                translator,
                simplifier,
                (c -> c.isEquation() ? numericalSievesMethod : numericalSievesMethodRelations),
                SymbolicEvaluator.getMaplePrevAfterCommands(),
                mapleScripts,
                config
        );

        evaluator.isMaple = true;

        return evaluator;
    }

    public static NumericalEvaluator createStandardMathematicaEvaluator() throws IOException, ComputerAlgebraSystemEngineException {
        NumericalConfig config =  NumericalConfig.config();
        MathematicaTranslator translator = new MathematicaTranslator();
        translator.init();

        String script = Util.getProcedure(GlobalPaths.PATH_MATHEMATICA_NUMERICAL_PROCEDURES);

        NumericalEvaluator evaluator = new NumericalEvaluator<Expr>(
                translator,
                translator,
                translator,
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
