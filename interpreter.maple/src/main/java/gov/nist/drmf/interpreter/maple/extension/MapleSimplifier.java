package gov.nist.drmf.interpreter.maple.extension;

import gov.nist.drmf.interpreter.common.cas.AbstractCasEngineSymbolicEvaluator;
import gov.nist.drmf.interpreter.common.cas.PackageWrapper;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.eval.EvaluatorType;
import gov.nist.drmf.interpreter.common.eval.SymbolicalConfig;
import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import gov.nist.drmf.interpreter.common.symbols.BasicFunctionsTranslator;
import gov.nist.drmf.interpreter.common.symbols.SymbolTranslator;
import gov.nist.drmf.interpreter.maple.common.SymbolicMapleEvaluatorTypes;
import gov.nist.drmf.interpreter.maple.listener.MapleListener;
import gov.nist.drmf.interpreter.maple.wrapper.MapleEngineFactory;
import gov.nist.drmf.interpreter.maple.wrapper.openmaple.Algebraic;
import gov.nist.drmf.interpreter.maple.wrapper.MapleException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author Andre Greiner-Petter
 */
public class MapleSimplifier extends AbstractCasEngineSymbolicEvaluator<Algebraic> {
    private static final Logger LOG = LogManager.getLogger(MapleSimplifier.class.getName());

    /**
     * This zero pattern allows expressions such as
     *  0 or 0. or 0.0 or 0.000 and so on.
     */
    private static final String ZERO_PATTERN = "0\\.?0*";

    private final MapleInterface maple;
    private final MapleListener listener;
    private final PackageWrapper packageWrapper;

    private double timeout = -1;

    public MapleSimplifier() {
        maple = MapleInterface.getUniqueMapleInterface();
        listener = MapleEngineFactory.getUniqueMapleListener();

        SymbolTranslator symbolTranslator = new SymbolTranslator(Keys.KEY_LATEX, Keys.KEY_MAPLE);
        BasicFunctionsTranslator basicFunctionsTranslator = new BasicFunctionsTranslator(Keys.KEY_MAPLE);
        try {
            symbolTranslator.init();
            basicFunctionsTranslator.init();
        } catch (IOException e) {
            LOG.fatal("Unable to initiate the symbol and function translator.", e);
        }
        packageWrapper = new PackageWrapper(basicFunctionsTranslator, symbolTranslator);

        // little hack, we need to load the configs on this VM too (the simplifier no longer share the same
        // SymbolicMapleEvaluatorTypes because they are in another VM)
        new SymbolicalConfig(SymbolicMapleEvaluatorTypes.values());
    }

    @Override
    public void setGlobalSymbolicAssumptions(List<String> assumptions) throws ComputerAlgebraSystemEngineException {
        String cmd = String.join(", ", assumptions);
        try {
            maple.evaluate("assume(" + cmd + ");");
            LOG.info("Set global assumptions in Maple: " + assumptions);
        } catch (MapleException me) {
            LOG.error("Unable to set global assumptions for Maple: " + assumptions);
            throw new ComputerAlgebraSystemEngineException(me);
        }
    }

    public void resetAssumptions(Set<String> variablesToReset) throws ComputerAlgebraSystemEngineException {
        StringBuilder cmd = new StringBuilder();
        for ( String var : variablesToReset ) {
            cmd.append(var).append(" := '").append(var).append("';").append(System.lineSeparator());
        }
        try {
            maple.evaluate(cmd.toString());
        } catch (MapleException me) {
            LOG.error("Unable to reset global assumptions for variables: " + variablesToReset);
            throw new ComputerAlgebraSystemEngineException(me);
        }
    }

    @Override
    public void setTimeout(EvaluatorType type, double timeLimit) {
        if ( EvaluatorType.SYMBOLIC.equals(type) ) this.timeout = timeLimit;
    }

    @Override
    public void setTimeout(double timeout) {
        this.timeout = timeout;
    }

    /**
     * This method takes two maple expressions and returns true when both expression
     * are symbolically the same. To verify this, we use the "simplify" command from
     * Maple. Be aware that both expressions still can be mathematically equivalent
     * even when this method returns false!
     *
     * Be also aware that null inputs always returns false, even when both inputs are null.
     * However, two empty expression such as "" and "" returns true.
     *
     * @param exp1 Maple string of the first expression
     * @param exp2 Maple string of the second expression
     * @return true if both expressions are symbolically equivalent or false otherwise.
     *          If it returns false, both expressions still can be mathematically equivalent!
     * @throws MapleException If the test of equivalence produces an Maple error.
     */
    public boolean isEquivalent( String exp1, String exp2 )
            throws ComputerAlgebraSystemEngineException {
        if ( isNullOrEmpty(exp1, exp2) ) return false;

        // otherwise build simplify command to test equivalence
        String command = "(" + exp1 + ") - (" + exp2 + ")";
        Algebraic a = simplify( command );
        try {
            return isZero(a);
        } catch ( MapleException me ) {
            throw new ComputerAlgebraSystemEngineException(me);
        }
    }

    /**
     * This method takes two maple expressions and converts the difference
     * to the specified function before it tries to simplify the difference.
     *
     * It works exactly in the same way as {@link #isEquivalent(String, String)},
     * but converts the difference of {@param exp1} and {@param exp2} before it tries
     * to simplify the new expression.
     *
     * @param exp1 Maple string of the first expression
     * @param exp2 Maple string of the second expression
     * @param conversion Specified the destination of the conversion. For example, "expe" or "hypergeom".
     * @return true if both expressions are symbolically equivalent or false otherwise.
     *          If it returns false, both expressions still can be mathematically equivalent!
     * @throws MapleException If the test of equivalence produces an Maple error.
     */
    public boolean isEquivalentWithConversion(
            String exp1,
            String exp2,
            String conversion )
            throws ComputerAlgebraSystemEngineException, MapleException {
        if ( isNullOrEmpty(exp1, exp2) ) return false;

        // otherwise build simplify command to test equivalence
        String command = "convert((" + exp1 + ") - (" + exp2 + "),"+ conversion +")";
        Algebraic a = simplify( command );
        return isZero(a);
    }

    public Algebraic isMultipleEquivalent( String exp1, String exp2 )
            throws ComputerAlgebraSystemEngineException {
        if ( isNullOrEmpty(exp1, exp2) ) return null;

        // otherwise build simplify command to test equivalence
        String command = "(" + exp1 + ") / (" + exp2 + ")";
        Algebraic a = simplify( command );
        return a;
    }

    public Algebraic isMultipleEquivalentWithConversion(
            String exp1,
            String exp2,
            String conversion )
            throws ComputerAlgebraSystemEngineException{
        if ( isNullOrEmpty(exp1, exp2) ) return null;

        // otherwise build simplify command to test equivalence
        String command = "convert((" + exp1 + ") / (" + exp2 + "),"+ conversion +")";
        return simplify( command );
    }

    public boolean isEquivalentWithExpension(
            String exp1,
            String exp2,
            String conversion
    ) throws ComputerAlgebraSystemEngineException, MapleException {
        if ( isNullOrEmpty(exp1, exp2) ) return false;

        // otherwise build simplify command to test equivalence
        String command = "expand((" + exp1 + ") - (" + exp2 + ")";
        command += conversion == null ? ")" : "," + conversion + ")";
        Algebraic a = simplify( command );
        return isZero(a);
    }

    public Algebraic isMultipleEquivalentWithExpension(
            String exp1,
            String exp2,
            String conversion
    ) throws ComputerAlgebraSystemEngineException {
        if ( isNullOrEmpty(exp1, exp2) ) return null;

        // otherwise build simplify command to test equivalence
        String command = "expand((" + exp1 + ") / (" + exp2 + ")";
        command += conversion == null ? ")" : "," + conversion + ")";
        return simplify( command );
    }

    /**
     * Checks if the given algebraic object is 0.
     * @param a an algebraic object
     * @return true if the result is 0. False otherwise.
     * @throws MapleException if the given command produces an error in Maple.
     */
    public static boolean isZero( Algebraic a ) throws MapleException {
        // null solutions returns false
        if ( a == null || a.isNULL() ) return false;
        // analyze the output string and returns true when it matches "0".
        String solution_str = a.toString();
        return solution_str.trim().matches(ZERO_PATTERN);
    }

    /**
     * If one of them is null, returns true.
     * If none is null but one of them is empty, it returns true
     * when both are empty, otherwise false.
     * Otherwise returns false.
     * @param exp1 string
     * @param exp2 string
     * @return true or false
     */
    public static boolean isNullOrEmpty( String exp1, String exp2 ){
        // test if one of the inputs is null
        if ( exp1 == null || exp2 == null ) return true;
        // if one of the expressions is empty, it only returns true when both are empty
        if ( exp1.isEmpty() || exp2.isEmpty() ){
            return !(exp1.isEmpty() && exp2.isEmpty());
        }
        return false;
    }

    private String latestTestExpression = "";

    @Override
    public String getLatestTestExpression() {
        return latestTestExpression;
    }

    /**
     * Simplify given expression. Be aware, the given expression should not
     * end with ';'.
     * @param maple_expr given maple expression, without ';'
     * @return the algebraic object of the result of simplify(maple_expr);
     * @throws MapleException if the given expression cannot be evaluated.
     * @see Algebraic
     */
    public Algebraic mapleSimplify( String maple_expr, Set<String> requiredPackages ) throws MapleException {
        latestTestExpression = "";
        String simplify = chooseSimplify(requiredPackages);

        if ( requiredPackages != null && !requiredPackages.isEmpty() ) {
            String loadCommands = packageWrapper.loadPackages(requiredPackages);
            maple.evaluate(loadCommands);
            LOG.debug("Loaded packages: " + requiredPackages);
        }

        String command = simplify + "(" + maple_expr + ")";
        latestTestExpression = command;
        if ( timeout > 0 ) {
            command = "try timelimit("+timeout+","+command+"); catch \"time expired\": \"";
            command += MapleInterface.TIMED_OUT_SIGNAL;
            command += "\"; end try;";
        } else command += ";";

        LOG.debug("Simplification: " + command);
        listener.timerReset();
        Algebraic result = maple.evaluate(command);

        if ( requiredPackages != null && !requiredPackages.isEmpty() ) {
            String unloadCommands = packageWrapper.unloadPackages(requiredPackages);
            maple.evaluate(unloadCommands);
            LOG.debug("Unloaded packages: " + requiredPackages);
        }

        return result;
    }

    private String chooseSimplify(Set<String> requiredPackages) {
        if ( requiredPackages == null || requiredPackages.isEmpty() )
            return "simplify";

        boolean contained = requiredPackages.removeIf(p -> p.contains("QDifferenceEquations"));;

        if ( contained ) {
            requiredPackages.add("QDifferenceEquations");
            return "QSimplify";
        }
        else return "simplify";
    }

    private Algebraic simplify(String expr) throws ComputerAlgebraSystemEngineException {
        return simplify(expr, new TreeSet<>());
    }

    @Override
    public Algebraic simplify(String expr, Set<String> requiredPackages) throws ComputerAlgebraSystemEngineException {
        try {
            return mapleSimplify(expr, requiredPackages);
        } catch ( MapleException me ) {
            throw new ComputerAlgebraSystemEngineException(me);
        }
    }

    private String buildAssumeSimplify(String expr, String assumption, Set<String> requiredPackages) {
        String simplify = chooseSimplify(requiredPackages);
        String cmd = simplify+"(" + expr + ")";//+ assuming " + assumption + ";";

        if ( timeout > 0 ) {
            cmd = "try timelimit("+timeout+", "+ cmd+") ";
            cmd += "assuming " + assumption + "; ";
            cmd += "catch \"time expired\": \"";
            cmd += MapleInterface.TIMED_OUT_SIGNAL;
            cmd += "\"; end try;";
        } else cmd += "assuming " + assumption + ";";

        cmd = packageWrapper.addPackages(cmd, requiredPackages);

        return cmd;
    }

    @Override
    public Algebraic simplify(String expr, String assumption, Set<String> requiredPackages) throws ComputerAlgebraSystemEngineException {
        try {
            String cmd = buildAssumeSimplify(expr, assumption, requiredPackages);
            LOG.debug("Simplification: " + cmd);
            listener.timerReset();
            return maple.evaluate( cmd );
        } catch ( MapleException me ) {
            throw new ComputerAlgebraSystemEngineException(me);
        }
    }

    @Override
    public boolean isTrue(Algebraic in) throws ComputerAlgebraSystemEngineException {
        try {
            Algebraic boolResult = maple.evaluate( "evalb(" + in.toString() + ");" );
            return "true".equals(boolResult.toString());
        } catch (MapleException e) {
            throw new ComputerAlgebraSystemEngineException(e);
        }
    }

    @Override
    public boolean isAsExpected(Algebraic in, double expect) {
        String str = in.toString();
        try {
            double res = Double.parseDouble(str);
            return res == expect;
        } catch ( NumberFormatException nfe ) {
            return false;
        }
    }

    @Override
    public boolean isConditionallyExpected(Algebraic in, double expect) {
        return false;
    }

    @Override
    public String getCondition(Algebraic in) {
        return "";
    }

    @Override
    public boolean wasAborted(Algebraic result) {
        return maple.isAbortedExpression(result);
    }
}
