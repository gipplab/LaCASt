package gov.nist.drmf.interpreter.pom.common;

import gov.nist.drmf.interpreter.pom.common.grammar.Brackets;
import gov.nist.drmf.interpreter.pom.common.grammar.ExpressionTags;
import gov.nist.drmf.interpreter.pom.common.grammar.MathTermTags;
import gov.nist.drmf.interpreter.pom.extensions.PrintablePomTaggedExpression;
import mlp.MathTerm;
import mlp.PomTaggedExpression;

/**
 * @author Andre Greiner-Petter
 */
public final class FakeMLPGenerator {
    private FakeMLPGenerator(){}

    /**
     * Use {@link #generateMathTermEmptyPPTE(MathTermTags, String)} instead
     * @return an empty sequence pom tagged expression
     */
    @Deprecated
    public static PomTaggedExpression generateMathTermEmptyPTE(MathTermTags tag, String term) {
        MathTerm mt = new MathTerm(term, tag.toString());
        return new PomTaggedExpression(mt);
    }

    /**
     * Use {@link #generateEmptySequencePPTE()} instead
     * @return an empty sequence pom tagged expression
     */
    @Deprecated
    public static PomTaggedExpression generateEmptySequencePTE() {
        return new PomTaggedExpression(generateEmptyMathTerm(), ExpressionTags.sequence.tag());
    }

    public static PrintablePomTaggedExpression generateEmptyPPTE() {
        return new PrintablePomTaggedExpression(generateEmptyMathTerm());
    }

    public static PrintablePomTaggedExpression generateMathTermEmptyPPTE(MathTermTags tag, String term) {
        MathTerm mt = new MathTerm(term, tag.toString());
        return new PrintablePomTaggedExpression(mt, ExpressionTags.sequence.tag());
    }

    public static PrintablePomTaggedExpression generateEmptySequencePPTE() {
        return new PrintablePomTaggedExpression(generateEmptyMathTerm(), ExpressionTags.sequence.tag());
    }

    /**
     * Use {@link #generateEmptyBinomialCoefficientPPTE()} instead
     * @return an empty sequence pom tagged expression
     */
    @Deprecated
    public static PomTaggedExpression generateEmptyBinomialCoefficientPTE() {
        return new PomTaggedExpression(generateEmptyMathTerm(), ExpressionTags.binomial.tag());
    }

    public static PrintablePomTaggedExpression generateEmptyBinomialCoefficientPPTE() {
        return new PrintablePomTaggedExpression(generateEmptyMathTerm(), ExpressionTags.binomial.tag());
    }

    public static PomTaggedExpression wrapNonSequenceInSequence(PomTaggedExpression pte) {
        if ( PomTaggedExpressionUtility.isSequence(pte) ) return pte;

        PomTaggedExpression sequence;
        if ( pte instanceof PrintablePomTaggedExpression ) {
            sequence = generateEmptySequencePPTE();
        } else sequence = generateEmptySequencePTE();
        sequence.addComponent(pte);
        return sequence;
    }

    public static MathTerm generateEmptyMathTerm() {
        return new MathTerm(" ");
    }

    public static MathTerm generateClosedParenthesesMathTerm() {
        return new MathTerm(Brackets.right_parenthesis.symbol, MathTermTags.right_parenthesis.tag());
    }

    public static MathTerm generateBracket(Brackets b) {
        MathTerm mt = new MathTerm(b.symbol, b.mathTermTag.tag());
        mt.setNamedFeature(FeatureSetUtility.LATEX_FEATURE_KEY, b.symbol);
        return mt;
    }

    public static PomTaggedExpression generateBracketExpression(Brackets b) {
        MathTerm mt = generateBracket(b);
        return new PrintablePomTaggedExpression(mt);
    }
}
