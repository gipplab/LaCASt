package gov.nist.drmf.interpreter.mlp.extensions;

import gov.nist.drmf.interpreter.common.grammar.Brackets;
import gov.nist.drmf.interpreter.common.grammar.ExpressionTags;
import gov.nist.drmf.interpreter.common.grammar.MathTermTags;
import mlp.MathTerm;
import mlp.PomTaggedExpression;

/**
 * @author Andre Greiner-Petter
 */
public final class FakeMLPGenerator {
    private FakeMLPGenerator(){}

    public static PomTaggedExpression generateEmptySequencePTE() {
        return new PomTaggedExpression(generateEmptyMathTerm(), ExpressionTags.sequence.tag());
    }

    public static PomTaggedExpression generateEmptyBinomialCoefficientPTE() {
        return new PomTaggedExpression(generateEmptyMathTerm(), ExpressionTags.binomial.tag());
    }

    public static MathTerm generateEmptyMathTerm() {
        return new MathTerm("");
    }

    public static MathTerm generateClosedParenthesesMathTerm() {
        return new MathTerm(Brackets.right_parenthesis.symbol, MathTermTags.right_parenthesis.tag());
    }
}
