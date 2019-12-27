package gov.nist.drmf.interpreter;

import com.wolfram.jlink.Expr;
import com.wolfram.jlink.MathLinkException;
import gov.nist.drmf.interpreter.cas.translation.SemanticLatexTranslator;
import gov.nist.drmf.interpreter.common.constants.GlobalPaths;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.grammar.IComputerAlgebraSystemEngine;
import gov.nist.drmf.interpreter.constraints.IConstraintTranslator;
import gov.nist.drmf.interpreter.evaluation.ICASEngineNumericalEvaluator;
import gov.nist.drmf.interpreter.evaluation.ICASEngineSymbolicEvaluator;
import gov.nist.drmf.interpreter.evaluation.INumericalEvaluationScripts;
import gov.nist.drmf.interpreter.mathematica.MathematicaInterface;
import gov.nist.drmf.interpreter.mathematica.common.Commands;
import gov.nist.drmf.interpreter.mathematica.evaluate.SymbolicEquivalenceChecker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;
import java.util.Observable;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Andre Greiner-Petter
 */
public class MathematicaTranslator implements
        IConstraintTranslator,
        IComputerAlgebraSystemEngine<Expr>,
        ICASEngineSymbolicEvaluator<Expr>,
        ICASEngineNumericalEvaluator<Expr>
{
    private static final Logger LOG = LogManager.getLogger(MathematicaTranslator.class.getName());

    public static final String MATH_ABORTION_SIGNAL = "$Aborted";

    /**
     * Interface to Mathematica
     */
    private MathematicaInterface mi;

    private SymbolicEquivalenceChecker miEquiChecker;

    /**
     * The interface to interact with the DLMF LaTeX translator
     */
    private SemanticLatexTranslator dlmfInterface;

    public MathematicaTranslator() {}

    public void init() throws IOException {
        // setup logging
        System.setProperty( Keys.KEY_SYSTEM_LOGGING, GlobalPaths.PATH_LOGGING_CONFIG.toString() );

        mi = MathematicaInterface.getInstance();
        miEquiChecker = mi.getEvaluationChecker();
        LOG.debug("Initialized Mathematica Interface");


        dlmfInterface = new SemanticLatexTranslator( Keys.KEY_MATHEMATICA );
        dlmfInterface.init( GlobalPaths.PATH_REFERENCE_DATA );
        LOG.debug("Initialized DLMF LaTeX Interface.");
    }

    @Override
    public String translate(String expression, String label) throws TranslationException {
        return dlmfInterface.translate(expression, label);
    }

    @Override
    public Expr enterCommand(String command) throws ComputerAlgebraSystemEngineException {
        try {
            return mi.evaluateToExpression(command);
//            return mi.evaluate(command);
        } catch (MathLinkException e) {
            throw new ComputerAlgebraSystemEngineException(e);
        }
    }

    @Override
    public void forceGC() throws ComputerAlgebraSystemEngineException {
        // nothing to do here, hopefully we don't need to invoke GC for mathematica
    }

    @Override
    public Expr simplify(String expr) throws ComputerAlgebraSystemEngineException {
        try {
            return miEquiChecker.fullSimplify(expr);
        } catch (MathLinkException e) {
            throw new ComputerAlgebraSystemEngineException(e);
        }
    }

    @Override
    public Expr simplify(String expr, String assumption) throws ComputerAlgebraSystemEngineException {
        try {
            return miEquiChecker.fullSimplify(expr, assumption);
        } catch (MathLinkException e) {
            throw new ComputerAlgebraSystemEngineException(e);
        }
    }

    @Override
    public boolean isAsExpected(Expr in, String expect) {
        try {
            return miEquiChecker.testZero(in);
        } catch (MathLinkException e) {
            LOG.error("Cannot check if expression is zero " + in.toString());
            return false;
        }
    }

    @Override
    public void abort() {
        miEquiChecker.abort();
    }

    @Override
    public boolean wasAborted(Expr result) {
        return result.toString().matches(Pattern.quote(MATH_ABORTION_SIGNAL));
    }

    @Override
    public String buildList(List<String> list) {
        String ls = list.toString();
        return ls.substring(1, ls.length()-1);
    }

    @Override
    public void update(Observable o, Object arg) {
        // nothing to do here
    }

    /**
     * Mathematica Numerical Tests Workflow:
     *
     * 1) Get Variables in Expression
     * 2) In case of Constraints
     *  2.1) ConstVars from Variable
     *  2.2) Get ConstVars - Value Pairs
     * 3) In Case of special values
     *  3.1) Special Variables from Variables
     *  3.2) Special Variables - Value Pairs
     * 4) Rest of Variables - Value pairs
     * 5) Variables - Values paris
     */
    private static final String NL = System.lineSeparator();
    private StringBuilder sb;

    /**
     * Extracts variables and stores them.
     * @param expression mathematical expression (already translated)
     * @param testValues list of values
     * @return variable name
     */
    @Override
    public String storeVariables(String expression, List<String> testValues) {
        String varName = "vars";
        String valsName = generateValuesVarName(varName);

        sb = new StringBuilder();
        String extract = Commands.EXTRACT_VARIABLES.build(expression);
        addVarDefinitionNL(sb, varName, extract);
        addVarDefinitionNL(sb, valsName, buildMathList(testValues));
        return varName;
    }

    @Override
    public String storeConstraintVariables(String variableName, List<String> constraintVariables, List<String> constraintValues) {
        String eVars = "constVars";
        String eVals = generateValuesVarName(eVars);
        addVarDefinitionNL(sb, eVars, buildMathList(constraintVariables));
        addVarDefinitionNL(sb, eVals, buildMathList(constraintValues));
        return eVars;
    }

    @Override
    public String storeExtraVariables(String variableName, List<String> extraVariables, List<String> extraValues) {
        String eVars = "extraVars";
        String eVals = generateValuesVarName(eVars);
        addVarDefinitionNL(sb, eVars, buildMathList(extraVariables));
        addVarDefinitionNL(sb, eVals, buildMathList(extraValues));
        return eVars;
    }

    @Override
    public String setConstraints(List<String> constraints) {
        String cons = "assumptions";
        addVarDefinitionNL(sb, cons, buildMathList(constraints));
        return cons;
    }

    @Override
    public String buildTestCases(String constraintsName, String variableNames, String constraintVariableNames, String extraVariableNames, int maxCombis) throws ComputerAlgebraSystemEngineException, IllegalArgumentException {
        String testCasesVar = "testCases";

        // create test cases first
        String testCasesCmd = Commands.CREATE_TEST_CASES.build(
                variableNames,
                generateValuesVarName(variableNames),
                constraintVariableNames,
                generateValuesVarName(constraintVariableNames),
                extraVariableNames,
                generateValuesVarName(extraVariableNames)
        );

        // filter cases based on constraints
        testCasesCmd = Commands.FILTER_TEST_CASES.build(constraintsName, testCasesCmd);
        addVarDefinitionNL(sb, testCasesVar, testCasesCmd);

        // check if number of test cases is below definition
        String lengthCmd = Commands.LENGTH_OF_LIST.build(testCasesVar);
        sb.append(lengthCmd);

        String commandString = sb.toString();
        LOG.trace("Numerical Test Commands:"+NL+commandString);

        try {
            String res = mi.evaluate(commandString);
            LOG.debug("Generated test cases: " + res);
            int nT = Integer.parseInt(res);
            if ( nT > maxCombis ) throw new IllegalArgumentException("Too many test combinations.");
            // res should be an integer, testing how many test commands there are!
        } catch (MathLinkException | NumberFormatException e) {
            throw new ComputerAlgebraSystemEngineException(e);
        }

        return testCasesVar;
    }

    @Override
    public Expr performNumericalTests(String expression, String testCasesName, String postProcessingMethodName, int precision) throws ComputerAlgebraSystemEngineException {
        try {
            String cmd = Commands.NUMERICAL_TEST.build(expression, testCasesName);
            LOG.info("Compute numerical test for " + expression);
            return mi.evaluateToExpression(cmd);
        } catch (MathLinkException e) {
            throw new ComputerAlgebraSystemEngineException(e);
        }
    }

    @Override
    public ResultType getStatusOfResult(Expr results) throws ComputerAlgebraSystemEngineException {
        LOG.info("Numerical test finished. Result: " + results.toString());
        return null;
    }

    private void addVarDefinitionNL(StringBuilder sb, String varName, String def) {
        sb.append(varName).append(" := ").append(def).append(";").append(NL);
    }

    private static String generateValuesVarName(String var) {
        return var + "Vals";
    }

    private static String buildMathList(List<String> list) {
        if ( list == null || list.isEmpty() ) return "{}";
        String l = MapleSimplifier.makeListWithDelimiter(list);
        return "{"+l+"}";
    }

    public String getNumericalProcedures() {
        try (Stream<String> stream = Files.lines(GlobalPaths.PATH_MATHEMATICA_NUMERICAL_PROCEDURES ) ){
            String procedures = stream.collect( Collectors.joining(System.lineSeparator()) );
            stream.close(); // not really necessary
            LOG.debug("Successfully loaded procedures");
            return procedures;
        } catch (IOException ioe){
            LOG.error("Cannot load mathematica procedure file.", ioe);
            return null;
        }
    }

    @Override
    public String generateNumericalTestExpression(String input) {
        return input;
    }
}
