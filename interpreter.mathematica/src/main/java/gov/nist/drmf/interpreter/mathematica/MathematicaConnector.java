package gov.nist.drmf.interpreter.mathematica;

import com.wolfram.jlink.Expr;
import gov.nist.drmf.interpreter.common.cas.CASProcedureLoader;
import gov.nist.drmf.interpreter.common.cas.ICASEngineNumericalEvaluator;
import gov.nist.drmf.interpreter.common.cas.ICASEngineSymbolicEvaluator;
import gov.nist.drmf.interpreter.common.cas.IComputerAlgebraSystemEngine;
import gov.nist.drmf.interpreter.common.constants.GlobalPaths;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.eval.ISymbolicTestCases;
import gov.nist.drmf.interpreter.common.eval.NativeComputerAlgebraInterfaceBuilder;
import gov.nist.drmf.interpreter.common.exceptions.CASUnavailableException;
import gov.nist.drmf.interpreter.mathematica.common.SymbolicMathematicaEvaluatorTypes;
import gov.nist.drmf.interpreter.mathematica.config.MathematicaConfig;
import gov.nist.drmf.interpreter.mathematica.extension.MathematicaInterface;
import gov.nist.drmf.interpreter.mathematica.extension.MathematicaNumericalCalculator;
import gov.nist.drmf.interpreter.mathematica.extension.MathematicaSimplifier;

/**
 * @author Andre Greiner-Petter
 */
public class MathematicaConnector implements NativeComputerAlgebraInterfaceBuilder<Expr> {
    private Boolean casIsAvailable = null;

    private MathematicaNumericalCalculator numericalCalculator = null;
    private MathematicaSimplifier symbolicCalculator = null;

    public MathematicaConnector() {}

    @Override
    public boolean isCASAvailable() {
        if ( casIsAvailable == null ) return casIsAvailable = MathematicaConfig.isMathematicaPresent();
        return casIsAvailable;
    }

    @Override
    public String getLanguageKey() {
        return Keys.KEY_MATHEMATICA;
    }

    @Override
    public IComputerAlgebraSystemEngine<Expr> getCASEngine() throws CASUnavailableException {
        if ( !isCASAvailable() ) throw new CASUnavailableException();
        return MathematicaInterface.getInstance();
    }

    @Override
    public ICASEngineSymbolicEvaluator<Expr> getSymbolicEvaluator() throws CASUnavailableException {
        if ( !isCASAvailable() ) throw new CASUnavailableException();
        if ( symbolicCalculator == null ) symbolicCalculator = new MathematicaSimplifier();
        return symbolicCalculator;
    }

    @Override
    public ICASEngineNumericalEvaluator<Expr> getNumericEvaluator() throws CASUnavailableException {
        if ( !isCASAvailable() ) throw new CASUnavailableException();
        if ( numericalCalculator == null ) numericalCalculator = new MathematicaNumericalCalculator();
        return numericalCalculator;
    }

    @Override
    public ISymbolicTestCases[] getDefaultSymbolicTestCases() {
        return SymbolicMathematicaEvaluatorTypes.values();
    }

    @Override
    public String[] getNumericProcedures() {
        String script = CASProcedureLoader.getProcedure(GlobalPaths.PATH_MATHEMATICA_NUMERICAL_PROCEDURES);
        return new String[]{script};
    }
}
