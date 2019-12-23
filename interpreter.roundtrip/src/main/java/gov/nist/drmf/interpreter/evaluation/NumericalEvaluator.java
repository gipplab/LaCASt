package gov.nist.drmf.interpreter.evaluation;

import com.maplesoft.externalcall.MapleException;
import com.maplesoft.openmaple.Algebraic;
import gov.nist.drmf.interpreter.MapleSimplifier;
import gov.nist.drmf.interpreter.MapleTranslator;
import gov.nist.drmf.interpreter.common.constants.GlobalPaths;
import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.grammar.IComputerAlgebraSystemEngine;
import gov.nist.drmf.interpreter.constraints.Constraints;
import gov.nist.drmf.interpreter.constraints.IConstraintTranslator;
import gov.nist.drmf.interpreter.maple.common.MapleConstants;
import gov.nist.drmf.interpreter.maple.listener.MapleListener;
import gov.nist.drmf.interpreter.maple.translation.MapleInterface;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.omg.PortableInterceptor.SUCCESSFUL;

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
@SuppressWarnings("ALL")
public class NumericalEvaluator<T> extends AbstractNumericalEvaluator<T> {//implements Observer {

    private static final Logger LOG = LogManager.getLogger(NumericalEvaluator.class.getName());

//    protected static final String LONG_RUNTIME_SKIP = "89,90,91,99,100,102";
    //TODO MATHEMATICA SKIPS
    protected static final String LONG_RUNTIME_SKIP =
        "103,402,640,649,1248,1315,1316,1317,1318,1319,1320,1321,1322,1323,1324,1325,1326,1410," +
                "1445,1460,1461,1462,1463,1464,1465,1466,1467,1468,1469,1470,1471,1542," +
                "2068,2498,2562,2563,2564,2565,2566," +
                "3035,3358,3494,3816,3817," +
                "4212,4213,4214,4485,4486,4818,4831,4832,4964," +
                "5061,5062,5063,5064";

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

        Set<ID> skips = getFailures(config.getSymbolicResultsPath());

        if ( skips != null ) {
            LOG.info("Symbolic results are specified. Ignore specified subset and test for values in the result data.");
            AbstractEvaluator.ID min = skips.stream().min( (u,v) -> u.id.compareTo(v.id)).get();
            AbstractEvaluator.ID max = skips.stream().max( (u,v) -> u.id.compareTo(v.id)).get();
            subset = new int[]{min.id, max.id + 1};
        }

        LinkedList<Case> testCases = loadTestCases(
                subset,
                skips,
                config.getDataset(),
                labelLib,
                skippedLinesInfo,
                true
        );

        lineResult = new LinkedList[subset[1]];
        for ( Integer i : skippedLinesInfo.keySet() ){
            lineResult[i] = new LinkedList<>();
            lineResult[i].add(skippedLinesInfo.get(i));
        }

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

        if ( lineResult[c.getLine()] == null ){
            lineResult[c.getLine()] = new LinkedList();
        }

        if ( c instanceof AbstractEvaluator.DummyCase ) {
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

            ICASEngineNumericalEvaluator.ResultType resType = testResult(results);
            String evaluation = "";
            switch (resType) {
                case SUCCESS:
                    lineResult[c.getLine()].add("Successful");
                    Status.SUCCESS.add();
                    break;
                case FAILURE:
                    LOG.info("Test was NOT successful.");
                    evaluation = results.toString();
                    lineResult[c.getLine()].add(evaluation);
                    Status.FAILURE.add();
                    break;
                case ERROR:
                    LOG.info("Test was NOT successful.");
                    evaluation = results.toString();
                    lineResult[c.getLine()].add(evaluation);
                    Status.ERROR.add();
                    break;
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

        return config.getTestExpression( mapleLHS, mapleRHS );
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

        return evaluator;
    }

    private static void startTestAndWriteResults( NumericalEvaluator evaluator ) throws IOException {
        LinkedList<Case> tests = evaluator.loadTestCases();
        evaluator.performAllTests(tests);
        evaluator.writeResults();
    }

    public static void main(String[] args) throws Exception{
        NumericalEvaluator evaluator = createStandardMapleEvaluator();
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
