package gov.nist.drmf.interpreter.mlp;

import gov.nist.drmf.interpreter.common.TeXPreProcessor;
import gov.nist.drmf.interpreter.common.constants.GlobalPaths;
import gov.nist.drmf.interpreter.common.grammar.Brackets;
import gov.nist.drmf.interpreter.common.grammar.ExpressionTags;
import gov.nist.drmf.interpreter.common.grammar.MathTermTags;
import gov.nist.drmf.interpreter.mlp.extensions.PrintablePomTaggedExpression;
import mlp.*;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * A simple wrapper class to parse LaTeX expression via the PoM-Tagger.
 * Since the PoM-Tagger is not able to reproduce the input string
 * based on the given parsed tree, this wrapper class works with a
 * custom extension of the {@link PomTaggedExpression}, name
 * {@link PrintablePomTaggedExpression}. This class wraps the
 * general PoM-Tagger (no semantic macros). If you need support for semantic
 * macros, use {@link SemanticMLPWrapper} instead.
 *
 * @see SemanticMLPWrapper
 * @see PrintablePomTaggedExpression
 * @see PomTaggedExpression
 *
 * @author Andre Greiner-Petter
 */
public class MLPWrapper {
    public static final byte NORMALIZE_SUB_SUPERSCRIPTS = 0b0001;
    public static final byte NORMALIZE_PARENTHESES = 0b0010;

    /**
     * The PoM-Parser object
     */
    private final PomParser parser;

    /**
     * A standard instance to ensure a better performance
     */
    private static MLPWrapper standardInstance;

    /**
     * Creates a non-semantic wrapper of the PomParser. The lexicon files are fetched from the
     * standard location {@link }
     */
    public MLPWrapper() {
        this(GlobalPaths.PATH_REFERENCE_DATA.toString());
    }

    /**
     * Creates a non-semantic wrapper of the PomParser with a given path to the lexicon files.
     * @param referenceDirPath the path to the "ReferenceData" folder
     */
    public MLPWrapper(String referenceDirPath) {
        this.parser = new PomParser(referenceDirPath);
    }

    /**
     * Adds a lexicon to the parser.
     * @param lexicon lexicon
     */
    public void addLexicon( Lexicon lexicon ) {
        parser.addLexicons(lexicon);
    }

    /**
     * Parses the given latex string to a {@link PomTaggedExpression}. It performs
     * string replacement rules prior to the parsing process (e.g., space deletions).
     * @param latex input string
     * @return tree structured parse tree
     * @throws ParseException if the given expression cannot be parsed
     */
    public PomTaggedExpression simpleParse(String latex) throws ParseException {
        latex = TeXPreProcessor.preProcessingTeX(latex); // clean input first
        return simpleParseRaw(latex);
    }

    public PomTaggedExpression simpleParse(String latex, String label) throws ParseException {
        latex = TeXPreProcessor.preProcessingTeX(latex, label); // clean input first
        return simpleParseRaw(latex);
    }

    public PomTaggedExpression simpleParseRaw(String latex) throws ParseException {
        return parser.parse(latex);
    }

    /**
     * Parses a given latex string to a printable {@link PomTaggedExpression}.
     * @param latex input string
     * @return printable version of a {@link PomTaggedExpression}
     * @throws ParseException if the given expression cannot be parsed
     */
    public PrintablePomTaggedExpression parse(String latex) throws ParseException {
        latex = TeXPreProcessor.preProcessingTeX(latex); // clean input first
        PomTaggedExpression pte = parser.parse(latex);
        return new PrintablePomTaggedExpression(pte, latex);
    }

    /**
     * Fully normalizes the given expression.
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

        ExpressionTags tag = ExpressionTags.getTagByKey(pte.getTag());
        MathTermTags mathTag = MathTermTags.getTagByKey(pte.getRoot().getTag());
        if ( ExpressionTags.sub_super_script.equals(tag) && (settings & NORMALIZE_SUB_SUPERSCRIPTS) != 0 ) {
            normalizeSubSuperScript(pte);
        } else if ( shouldNormalizeParenthesis(mathTag, settings) ) {
            Brackets orig = Brackets.getBracket(pte);
            Brackets normalized = Brackets.getBracket(orig.getAppropriateString());
            MathTerm newMT = FakeMLPGenerator.generateBracket(normalized);
            pte.setRoot(newMT);
        } else {
            pte.getComponents().forEach( p -> internalNormalize(p, settings) );
        }

        return pte;
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
     * Provide access to the standard instance of the PoM-Tagger. It increases the performances
     * if you keep the number of MLP instances low.
     * @return the standard instance of the this class
     */
    public static MLPWrapper getStandardInstance() {
        if ( standardInstance == null ) {
            standardInstance = new MLPWrapper();
        }
        return standardInstance;
    }

    /**
     * Helper method to quickly check if MLP is available or not.
     * Does not throw an exception if MLP is not available!
     * @return true if MLP (PoM-Tagger) is available, otherwise false.
     */
    public static boolean isMLPPresent() {
        try {
            return getStandardInstance() != null;
        } catch ( Exception e ) {
            return false;
        }
    }
}
