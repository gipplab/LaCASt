package gov.nist.drmf.interpreter.evaluation;

import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import gov.nist.drmf.interpreter.common.grammar.IComputerAlgebraSystemEngine;
import gov.nist.drmf.interpreter.common.grammar.ITranslator;
import gov.nist.drmf.interpreter.constraints.IConstraintTranslator;

/**
 * @author Andre Greiner-Petter
 */
public abstract class AbstractSymbolicEvaluator<T> extends AbstractEvaluator<T> {

    private ICASEngineSymbolicEvaluator<T> symbolicEvaluator;
    private ISymbolicTestCases[] symbolicTestCases;

    public AbstractSymbolicEvaluator(
            IConstraintTranslator forwardTranslator,
            IComputerAlgebraSystemEngine<T> engine,
            ICASEngineSymbolicEvaluator<T> symbolicEvaluator,
            ISymbolicTestCases[] testCases
    ) {
        super( forwardTranslator, engine );
        this.symbolicEvaluator = symbolicEvaluator;
        this.symbolicTestCases = testCases;
    }

    public T simplify( String command ) throws ComputerAlgebraSystemEngineException {
        return symbolicEvaluator.simplify(command);
    }

    public T simplify( String command, String assumption ) throws ComputerAlgebraSystemEngineException {
        if ( assumption == null || assumption.isEmpty() ) return symbolicEvaluator.simplify(command);
        return symbolicEvaluator.simplify(command, assumption);
    }

    public ISymbolicTestCases[] getSymbolicTestCases() {
        return this.symbolicTestCases;
    }

    public boolean validOutCome(T in, String expect) {
        return symbolicEvaluator.isAsExpected(in, expect);
    }


}
