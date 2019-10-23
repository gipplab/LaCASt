package gov.nist.drmf.interpreter.evaluation;

import com.maplesoft.externalcall.MapleException;
import com.maplesoft.openmaple.Algebraic;
import gov.nist.drmf.interpreter.MapleSimplifier;
import gov.nist.drmf.interpreter.MapleTranslator;
import gov.nist.drmf.interpreter.common.GlobalPaths;
import gov.nist.drmf.interpreter.common.TranslationException;
import gov.nist.drmf.interpreter.constraints.Constraints;
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

    private static final String SUC_SYMB = "57,59,61,63,79,82,83,130,161,163,206,215,293,294,311,312,313,314,322,336,337,362,366,370,371,372,373,374,375,377,379,381,383,385,387,414,415,430,432,437,439,441,443,462,487,517,518,524,525,526,536,538,557,558,569,571,574,575,576,585,586,588,654,676,678,717,718,730,733,734,735,736,737,738,739,740,741,742,743,744,745,746,747,748,749,755,762,763,764,765,767,769,770,771,772,774,775,779,780,802,803,804,805,820,823,824,825,826,827,828,829,830,831,832,833,834,835,836,837,850,851,853,854,856,860,861,915,984,987,988,989,990,999,1005,1006,1031,1042,1043,1044,1047,1049,1050,1051,1052,1058,1062,1067,1068,1070,1071,1072,1073,1074,1075,1076,1078,1079,1080,1081,1082,1083,1084,1085,1086,1087,1104,1109,1110,1111,1123,1127,1139,1145,1146,1147,1148,1149,1150,1151,1152,1153,1157,1158,1159,1171,1172,1173,1174,1175,1176,1177,1178,1182,1183,1184,1185,1186,1187,1188,1189,1190,1191,1192,1193,1194,1195,1196,1197,1198,1199,1200,1201,1202,1203,1204,1205,1206,1207,1208,1209,1210,1217,1218,1219,1220,1221,1222,1223,1224,1225,1226,1227,1228,1229,1230,1231,1232,1233,1234,1253,1254,1256,1257,1295,1303,1305,1306,1307,1312,1327,1328,1329,1330,1331,1332,1333,1334,1335,1336,1337,1338,1339,1340,1345,1346,1347,1348,1349,1350,1355,1356,1357,1358,1359,1360,1361,1362,1363,1364,1365,1366,1367,1368,1369,1370,1371,1372,1373,1374,1375,1376,1377,1378,1379,1383,1384,1385,1386,1387,1388,1389,1390,1391,1392,1393,1394,1395,1396,1397,1415,1419,1450,1453,1457,1473,1475,1477,1499,1503,1504,1505,1506,1507,1508,1509,1533,1535,1536,1537,1538,1541,1545,1546,1547,1548,1551,1552,1553,1563,1566,1568,1569,1571,1572,1573,1574,1580,1583,1800,1894,1895,1896,1897,1898,1899,1900,1901,1902,1903,1908,1909,1911,1912,1934,1935,1939,1940,1945,1946,1957,1958,1959,1961,2020,2021,2022,2025,2028,2030,2035,2036,2039,2040,2079,2080,2081,2116,2117,2118,2135,2140,2141,2142,2143,2146,2147,2149,2151,2152,2154,2158,2161,2164,2165,2166,2167,2168,2169,2171,2175,2176,2184,2185,2186,2187,2188,2189,2190,2191,2193,2194,2195,2230,2231,2234,2235,2236,2242,2246,2404,2405,2406,2407,2408,2416,2417,2423,2424,2425,2426,2427,2428,2429,2430,2431,2437,2438,2495,2496,2498,2499,2500,2501,2530,2532,2534,2536,2538,2540,2545,2546,2547,2548,2549,2550,2551,2552,2587,2588,2589,2590,2591,2592,2593,2594,2595,2596,2597,2598,2599,2600,2601,2602,2603,2604,2622,2623,2624,2625,2626,2627,2628,2629,2630,2631,2632,2633,2634,2635,2636,2637,2638,2639,2640,2641,2642,2643,2644,2645,2662,2667,2669,2781,2789,2796,2810,2812,2814,2815,2816,2817,2820,2840,2841,2849,2850,2851,2852,2854,2855,2856,2866,2872,2873,2876,2877,2878,2883,2884,2893,2894,2895,2948,2979,2980,2985,2986,3020,3022,3180,3212,3221,3222,3223,3224,3225,3226,3227,3228,3229,3230,3231,3232,3233,3234,3235,3236,3237,3238,3239,3240,3241,3242,3243,3244,3245,3246,3247,3248,3249,3250,3251,3252,3253,3254,3255,3256,3264,3265,3266,3267,3268,3269,3274,3275,3276,3291,3292,3293,3294,3303,3305,3306,3309,3310,3315,3317,3321,3461,3462,3464,3472,3487,3488,3489,3491,3494,3495,3496,3497,3513,3517,3716,3717,3749,3751,3753,3755,3759,3760,3761,3766,3767,3768,3769,3772,3773,3774,3776,3783,3784,3785,3851,3854,3857,3860,3866,3878,4032,4034,4035,4036,4037,4047,4049,4051,4053,4060,4066,4067";

    public static boolean SKIP_SUC_SYMB = true;

    // 0.6GB
    public static final long MEMORY_NOTIFY_LIMIT_KB = 500_000;

    public static final int MAX_LOG_LENGTH = 300;

    private static Path output;

    private static MapleTranslator translator;
    private MapleSimplifier simplifier;

    private NumericalConfig config;

    private LinkedList<Case> testCases;

    private HashMap<Integer, String> labelLib;

    private String[] lineResult;

    private String numericalSievesMethod;
    private String numericalSievesMethodRelations;

    private LinkedList<String> mapleScripts;

    static {
        translator = new MapleTranslator();
        try {
            LOG.debug("Initiate translation engine.");
            translator.init();
        } catch (Exception e) {
            LOG.fatal("Cannot initiate translation engine!", e);
        }
    }

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
        this.config = NumericalConfig.config();
        labelLib = new HashMap<>();

        this.testCases = new LinkedList<>();
        Status.reset();

        this.mapleScripts = new LinkedList<>();
    }

    public static MapleTranslator getTranslator(){
        return translator;
    }

    public void init() throws IOException, MapleException {
        LOG.info("Setup numerical tests...");
        output = config.getOutputPath();
        if (!Files.exists(output)) {
            Files.createFile(output);
        }

        simplifier = translator.getMapleSimplifier();

        //LOG.debug("Register for memory observer service.");
        //translator.addMapleMemoryObserver(this);
        //MapleListener.setMemoryUsageLimit( MEMORY_NOTIFY_LIMIT_KB );

        // load special numerical test maple procedure
        LOG.debug("Loading Maple internal procedures.");
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
        LOG.debug("Setup done!");
    }

    protected void setTranslator( MapleTranslator translator ){
        this.translator = translator;
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

        lineResult = new String[subset[1]];
        for ( Integer i : skippedLinesInfo.keySet() )
            lineResult[i] = skippedLinesInfo.get(i);
    }

    public static LinkedList<Case> loadTestCases(int[] subset, Path dataset, HashMap<Integer, String> labelLib, HashMap<Integer, String> skippedLinesInfo) {
        //int[] subset = config.getSubset();
        LinkedList<Case> testCases = new LinkedList<>();
        int[] currLine = new int[] {0};

        Set<Integer> successSymbs = new TreeSet();
        String[] sS = SUC_SYMB.split(",");
        for ( String s : sS )
            successSymbs.add(Integer.parseInt(s));

        try (BufferedReader br = Files.newBufferedReader(dataset)) {
            Stream<String> lines = br.lines();

            int start = subset[0];
            int limit = subset[1];

            lines   .peek(  l -> currLine[0]++) // line counter
                    .filter(l -> start <= currLine[0] && currLine[0] < limit) // filter by limits
//                    .filter(l -> {
//                        if ( !SKIP_SUC_SYMB ) return true;
//                        if ( successSymbs.contains(currLine[0]) ) {
//                            Status.SUCCESS_SYMB.add();
//                            skippedLinesInfo.put( currLine[0], "Skipped - Successful Symbolic Tests.");
//                            return false;
//                        }
//                        return true;
//                    })
                    .filter(l -> { // filter ' because of ambiguous meanings
                        if (l.contains("'")) {
//                            Case c = CaseAnalyzer.analyzeLine(l, currLine[0]);
//                            LOG.debug("Skip line " + currLine[0] + " because of '.");
//                            skippedLinesInfo.put( currLine[0], "Skipped - Because of ambiguous single quote." );
//                            if ( c != null ) labelLib.put( c.getLine(), c.getDlmf() );
//                            Status.SKIPPED.add();
                            System.out.println(currLine[0] + " SKIPPED because '.");
                            return false;
                        }
                        return true;
                    })
                    .map(l -> {
                        Case c = CaseAnalyzer.analyzeLine(l, currLine[0]);
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
            LOG.info("Start test for line: " + c.getLine());
            LOG.info("Test case: " + c);

            Constraints con =  c.getConstraintObject();
            if ( con != null ){
                if ( !con.specialValuesInfo().isEmpty() )
                    LOG.info(con.specialValuesInfo());
                LOG.info(con.constraintInfo());
            }

            String expression = getTestedExpression(c);
            Status.SUCCESS_TRANS.add();

            String[] preAndPostCommands = getPrevCommand( c.getLHS() + ", " + c.getRHS() );

            if ( preAndPostCommands[0] != null ){
                LOG.debug("Enter pre-testing commands: " + preAndPostCommands[0]);
                translator.enterMapleCommand(preAndPostCommands[0]);
            }

            LOG.debug("Start numerical calculations.");
            String resultsName = simplifier.advancedNumericalTest(
                    expression,
                    config.getNumericalValues(),
                    c.getConstraintVariables(),
                    c.getConstraintValues(),
                    config.getSpecialVariables(),
                    config.getSpecialVariablesValues(),
                    c.getConstraints(),
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
            if ( c.isEquation() ){
                sieveMethod = this.numericalSievesMethod + "(" + resultsName + ");";
            } else {
                sieveMethod = this.numericalSievesMethodRelations + "(" + resultsName + ");";
            }

            LOG.trace(sieveMethod);
            Algebraic results = translator.enterMapleCommand(sieveMethod);
            LOG.debug("Finished sieving... save outcome.");

            if ( results instanceof com.maplesoft.openmaple.List ) {
                com.maplesoft.openmaple.List aList = (com.maplesoft.openmaple.List) results;
                int l = aList.length();

                // if l == 0, the list is empty so the test was successful
                if ( l == 0 ){
                    LOG.info("Test was successful");
                    if ( lineResult == null ){
                        return "Successful";
                    }
                    lineResult[c.getLine()] = "Successful";
                    Status.SUCCESS.add();
                } else { // otherwise the list contains errors or simple failures
                    LOG.info("Test was NOT successful.");

                    String evaluation = aList.toString();

                    if ( lineResult == null ){
                        return evaluation;
                    }
                    lineResult[c.getLine()] = evaluation;

                    if ( evaluation.contains("Error") ){
                        Status.ERROR.add();
                    } else {
                        Status.FAILURE.add();
                    }
                }
            } else {
                LOG.warn("Sieved list was not a list object... " + results.toString());
                Status.ERROR.add();
            }
            LOG.info("Finished test for line: " + c.getLine());
        } catch ( TranslationException te ) {
            LOG.error("Error in translation. " + te.toString());
            lineResult[c.getLine()] = "Error - " + te.toString();
            Status.ERROR.add();
        } catch ( IllegalArgumentException iae ){
            LOG.warn("Skip test, because " + iae.getMessage());
            lineResult[c.getLine()] = "Skipped - " + iae.getMessage();
            Status.SKIPPED.add();
        } catch ( Exception e ){
            LOG.warn("Error for line " + c.getLine() + ", because: " + e.toString(), e);
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

    private String getTestedExpression(Case c){
        LOG.debug("Translating LHS: " + c.getLHS());
        String mapleLHS = translator.translateFromLaTeXToMapleClean( c.getLHS() );
        LOG.info("Translated LHS to: " + mapleLHS);

        LOG.debug("Translating RHS: " + c.getRHS());
        String mapleRHS = translator.translateFromLaTeXToMapleClean( c.getRHS() );
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

        return config.getTestExpression( mapleLHS, mapleRHS );
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
            performSingleTest(c);
            copy.add(c);
        }
        testCases = copy;
    }

    private void performMapleSessionRestart() throws RuntimeException {
        try {
            LOG.debug("Try to restart Maple session.");
            translator.restartMapleSession();
            LOG.debug("Reloading procedures...");
            reloadScripts();
            LOG.debug("Successfully restarted Maple session.");
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

    public static String[] translateEach(String[] texStr){
        return Arrays.stream(texStr)
                .map( translator::translateFromLaTeXToMapleClean )
                .toArray(String[]::new);
    }

    public static void main(String[] args) throws Exception{
//        NumericalEvaluator ne = new NumericalEvaluator();
//        ne.init();
//
//        String test = "m + n < 1 \\constraint{$m = 1,2,\\dots,\\floor{\\tfrac{1}{2}n}$}";
//
//        Case c = CaseAnalyzer.analyzeLine(test, 796);
//
//        System.out.println(c);
//
//        LOG.info(ne.performSingleTest(c));

        NumericalEvaluator evaluator = new NumericalEvaluator();
        evaluator.init();
        if(args.length>0){
            evaluator.testCases = new LinkedList<>();
            evaluator.testCases.add(
             //Note: Each instantiation of NumericalEvaluator overwrites the static variable labelLinker
             CaseAnalyzer.analyzeLine(args[0], 0)
            );
        } else {
            evaluator.loadTestCases();
        }
        evaluator.performAllTests();
        evaluator.writeOutput( evaluator.config.getOutputPath() );
    }

    @Override
    public void update(Observable o, Object arg) {
        LOG.info("Observed memory limit was reached. Restart maple session soon!");
        requestedRestart = true;
    }
}
