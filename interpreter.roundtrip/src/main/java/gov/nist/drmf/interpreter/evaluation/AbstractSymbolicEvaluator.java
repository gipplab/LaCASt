package gov.nist.drmf.interpreter.evaluation;

import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import gov.nist.drmf.interpreter.common.grammar.IComputerAlgebraSystemEngine;
import gov.nist.drmf.interpreter.common.grammar.ITranslator;

/**
 * @author Andre Greiner-Petter
 */
public abstract class AbstractSymbolicEvaluator<T> extends AbstractEvaluator<T> {

    private ICASEngineSymbolicEvaluator<T> symbolicEvaluator;

    public AbstractSymbolicEvaluator(
            ITranslator forwardTranslator,
            IComputerAlgebraSystemEngine<T> engine,
            ICASEngineSymbolicEvaluator<T> symbolicEvaluator
    ) {
        super( forwardTranslator, engine );
        this.symbolicEvaluator = symbolicEvaluator;
    }

    public T simplify( String command ) throws ComputerAlgebraSystemEngineException {
        return symbolicEvaluator.simplify(command);
    }

    public boolean validOutCome(String in, String expect) {
        return symbolicEvaluator.isAsExpected(in, expect);
    }


}
