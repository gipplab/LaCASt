package gov.nist.drmf.interpreter.evaluation;

import gov.nist.drmf.interpreter.common.grammar.IComputerAlgebraSystemEngine;
import gov.nist.drmf.interpreter.constraints.IConstraintTranslator;

/**
 * @author Andre Greiner-Petter
 */
public abstract class AbstractNumericalEvaluator<T> extends AbstractEvaluator<T> {

    public AbstractNumericalEvaluator(
            IConstraintTranslator forwardTranslator,
            IComputerAlgebraSystemEngine<T> engine
    ) {
        super(forwardTranslator, engine);
    }
}
