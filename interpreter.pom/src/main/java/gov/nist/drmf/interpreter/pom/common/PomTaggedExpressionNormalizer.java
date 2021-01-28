package gov.nist.drmf.interpreter.pom.common;

import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.pom.SemanticMLPWrapper;
import gov.nist.drmf.interpreter.pom.common.grammar.Brackets;
import gov.nist.drmf.interpreter.pom.common.grammar.ExpressionTags;
import gov.nist.drmf.interpreter.pom.common.grammar.FeatureValues;
import gov.nist.drmf.interpreter.pom.common.grammar.MathTermTags;
import mlp.MathTerm;
import mlp.PomTaggedExpression;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Andre Greiner-Petter
 */
public final class PomTaggedExpressionNormalizer {
    public static final byte NORMALIZE_SUB_SUPERSCRIPTS = 0b0001;
    public static final byte NORMALIZE_PARENTHESES = 0b0010;

    private static final SemanticMLPWrapper mlp = SemanticMLPWrapper.getStandardInstance();

    private PomTaggedExpressionNormalizer() {}

    /**
     * Fully normalizes the given expression. The normalization is performed on the
     * expression itself. Hence, the input will changed and returned. There will be
     * no new parse tree generated.
     * @param pte parsed tree that has to be normalized
     * @return fully normalized tree (all features activated)
     */
    public static PomTaggedExpression normalize(PomTaggedExpression pte) {
        return normalize(pte, NORMALIZE_PARENTHESES, NORMALIZE_SUB_SUPERSCRIPTS);
    }

    /**
     * Normalizes the given {@param pte}, e.g., order the sub-superscript elements {@link #NORMALIZE_PARENTHESES}.
     * It allows to add features to control the level of normalization.
     * @param pte parsed tree that has to be normalized
     * @param flags the levels that will be normalized
     * @return normalized expression
     */
    public static PomTaggedExpression normalize(PomTaggedExpression pte, byte... flags) {
        byte settings = settings(flags);
        return internalNormalize(pte, settings);
    }

    private static PomTaggedExpression internalNormalize(PomTaggedExpression pte, byte settings) {
        if ( settings == 0 ) return pte;

        normalizeAccents(pte);

        ExpressionTags tag = ExpressionTags.getTagByKey(pte.getTag());
        MathTermTags mathTag = MathTermTags.getTagByKey(pte.getRoot().getTag());
        if ( ExpressionTags.sub_super_script.equals(tag) && (settings & NORMALIZE_SUB_SUPERSCRIPTS) != 0 ) {
            normalizeSubSuperScript(pte);
        } else if ( shouldNormalizeParenthesis(mathTag, settings) ) {
            Brackets orig = Brackets.getBracket(pte);
            if ( orig != null ) {
                // this can happen when \left. \right|, in this case, we should not normalize it
                Brackets normalized = orig.getUnbalancedCounterpart();
                MathTerm newMT = FakeMLPGenerator.generateBracket(normalized);
                pte.setRoot(newMT);
            }
        } else {
            pte.getComponents().forEach( p -> internalNormalize(p, settings) );
        }

        return pte;
    }

    public static void normalizeAccents(PomTaggedExpression pte) {
        MathTermTags termTag = MathTermTags.getTagByExpression(pte);
        if ( MathTermTags.alphanumeric.equals(termTag) ) {
            List<String> accents = FeatureValues.ACCENT.getFeatureValues(pte);
            if ( accents.size() == 1 ) {
                pte.removeNamedFeature(Keys.FEATURE_ACCENT);
                pte.setNamedFeature(FeatureSetUtility.LATEX_FEATURE_KEY, "\\"+accents.get(0));
                pte.setSecondaryTags(ExpressionTags.accented.tag());
                pte.getRoot().setNamedFeature(Keys.FEATURE_ACCENT, accents.get(0));
            }
        }
    }

    public static void normalizeSubSuperScript(PomTaggedExpression pte) {
        ExpressionTags tag = ExpressionTags.getTagByKey(pte.getTag());
        if (!ExpressionTags.sub_super_script.equals(tag)) return;

        List<PomTaggedExpression> comps = pte.getComponents();
        PomTaggedExpression first = comps.get(0);
        MathTermTags mTag = MathTermTags.getTagByKey(first.getRoot().getTag());
        if ( MathTermTags.caret.equals(mTag) ) {
            // set components of PomTaggedExpression deletes the inner list of components first
            // Since our comps is the same as the deleted components list, the components are also
            // deleted from our list. Hence we must create a copy list and setComponents to this copy
            List<PomTaggedExpression> copy = new LinkedList<>(comps);
            Collections.reverse(copy);
            pte.setComponents(copy);
        }
    }

    private static boolean shouldNormalizeParenthesis(MathTermTags mathTag, byte settings) {
        return (MathTermTags.left_delimiter.equals(mathTag) || MathTermTags.right_delimiter.equals(mathTag)) &&
                (settings & NORMALIZE_PARENTHESES) != 0;
    }

    private static byte settings(byte... flags) {
        if (flags == null || flags.length == 0) return 0;

        byte tmp = 0;
        for ( byte b : flags ) {
            tmp += b;
        }
        return tmp;
    }

    /**
     * Internally changes the given expression by switching \pm and \mp to + or -. The second parameter specifies
     * which case you want to change it to. First case (true) means \pm becomes +, second case means it becomes -.
     * @param pte the pte to change
     * @param firstCase first case or second case mode
     * @return the updated expression without \pm and \mp
     */
    public static boolean normalizePm(PomTaggedExpression pte, boolean firstCase) {
        boolean encountered = false;
        if ( pte == null || pte.isEmpty() ) return false;
        if ( MathTermUtility.equals(pte.getRoot(), MathTermTags.operation) ) {
            MathTerm term = pte.getRoot();
            if ( "\\pm".equals(term.getTermText()) ) {
                if ( firstCase ) setMathTermPlus(term);
                else setMathTermMinus(term);
                pte.setRoot(term);
                encountered = true;
            } else if ( "\\mp".equals(term.getTermText()) ) {
                if ( firstCase ) setMathTermMinus(term);
                else setMathTermPlus(term);
                pte.setRoot(term);
                encountered = true;
            }
        } else if ( !pte.getComponents().isEmpty() ) {
            for ( PomTaggedExpression child : pte.getComponents() )
                encountered |= normalizePm(child, firstCase);
        }
        return encountered;
    }

    private static void setMathTermPlus(MathTerm term) {
        term.setTermText("+");
        term.setTag(MathTermTags.plus.tag());
        mlp.loadFeatures(term);
    }

    private static void setMathTermMinus(MathTerm term) {
        term.setTermText("-");
        term.setTag(MathTermTags.minus.tag());
        mlp.loadFeatures(term);
    }
}
