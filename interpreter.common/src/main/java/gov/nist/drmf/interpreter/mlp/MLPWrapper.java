package gov.nist.drmf.interpreter.mlp;

import gov.nist.drmf.interpreter.mlp.extensions.PrintablePomTaggedExpression;
import mlp.Lexicon;
import mlp.ParseException;
import mlp.PomParser;
import mlp.PomTaggedExpression;

import static gov.nist.drmf.interpreter.examples.MLP.GLOBAL_LEXICON_PATH;

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
    private PomParser parser;

    /**
     * Creates a non-semantic wrapper of the PomParser
     */
    public MLPWrapper() {
        this.parser = new PomParser(GLOBAL_LEXICON_PATH);
    }

    /**
     * Adds a lexicon to the parser.
     * @param lexicon lexicon
     */
    public void addLexicon( Lexicon lexicon ) {
        parser.addLexicons(lexicon);
    }

    /**
     * Parses the given latex string to a {@link PomTaggedExpression}.
     * @param latex input string
     * @return tree structured parse tree
     * @throws ParseException if the given expression cannot be parsed
     */
    public PomTaggedExpression simpleParse(String latex) throws ParseException {
        return parser.parse(latex);
    }

    /**
     * Parses a given latex string to a printable {@link PomTaggedExpression}.
     * @param latex input string
     * @return printable version of a {@link PomTaggedExpression}
     * @throws ParseException if the given expression cannot be parsed
     */
    public PrintablePomTaggedExpression parse(String latex) throws ParseException {
        PomTaggedExpression pte = simpleParse(latex);
        return new PrintablePomTaggedExpression(pte, latex);
    }

    /**
     * Helper method to quickly check if MLP is available or not.
     * Does not throw an exception if MLP is not available!
     * @return true if MLP (PoM-Tagger) is available, otherwise false.
     */
    public static boolean isMLPPresent() {
        try {
            new PomParser(GLOBAL_LEXICON_PATH);
            return true;
        } catch ( Exception e ) {
            return false;
        }
    }
}
