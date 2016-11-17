package gov.nist.drmf.interpreter.cas.parser;

import gov.nist.drmf.interpreter.cas.parser.AbstractParser;
import mlp.MathTerm;
import mlp.PomTaggedExpression;

/**
 * @author Andre Greiner-Petter
 */
public abstract class AbstractInnerParser extends AbstractParser {
    @Override
    public boolean parse(PomTaggedExpression expression) {
        return parse(expression.getRoot());
    }

    public abstract boolean parse(MathTerm term);
}
