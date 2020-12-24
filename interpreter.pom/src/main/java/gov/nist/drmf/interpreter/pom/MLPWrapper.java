package gov.nist.drmf.interpreter.pom;

import gov.nist.drmf.interpreter.common.latex.TeXPreProcessor;
import gov.nist.drmf.interpreter.common.constants.GlobalPaths;
import gov.nist.drmf.interpreter.pom.extensions.PrintablePomTaggedExpression;
import mlp.*;

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
public abstract class MLPWrapper {
    /**
     * The PoM-Parser object
     */
    private final PomParser parser;

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
    protected void addLexicon( Lexicon lexicon ) {
        parser.addLexicons(lexicon);
    }

    /**
     * Parses the given latex string to a {@link PomTaggedExpression}. It performs
     * string replacement rules prior to the parsing process (e.g., space deletions).
     * @param latex input string
     * @return tree structured parse tree
     * @throws ParseException if the given expression cannot be parsed
     */
    public synchronized PomTaggedExpression simpleParse(String latex) throws ParseException {
        latex = TeXPreProcessor.preProcessingTeX(latex); // clean input first
        return simpleParseRaw(latex);
    }

    public synchronized PomTaggedExpression simpleParse(String latex, String label) throws ParseException {
        latex = TeXPreProcessor.preProcessingTeX(latex, label); // clean input first
        return simpleParseRaw(latex);
    }

    public synchronized PomTaggedExpression simpleParseRaw(String latex) throws ParseException {
        return parser.parse(latex);
    }

    /**
     * Parses a given latex string to a printable {@link PomTaggedExpression}.
     * @param latex input string
     * @return printable version of a {@link PomTaggedExpression}
     * @throws ParseException if the given expression cannot be parsed
     */
    public synchronized PrintablePomTaggedExpression parse(String latex) throws ParseException {
        latex = TeXPreProcessor.preProcessingTeX(latex); // clean input first
        return parseRaw(latex);
    }

    public synchronized PrintablePomTaggedExpression parse(String latex, String label) throws ParseException {
        latex = TeXPreProcessor.preProcessingTeX(latex, label); // clean input first
        return parseRaw(latex);
    }

    public synchronized PrintablePomTaggedExpression parseRaw(String latex) throws ParseException {
        PomTaggedExpression pte = parser.parse(latex);
        return new PrintablePomTaggedExpression(pte, latex);
    }

    public synchronized MathTerm loadFeatures(MathTerm term) {
        Lexicon lex = parser.getLexicon();
        term.loadFeatureSets(lex);
        return term;
    }

    /**
     * Helper method to quickly check if MLP is available or not.
     * Does not throw an exception if MLP is not available!
     * @return true if MLP (PoM-Tagger) is available, otherwise false.
     */
    public static boolean isMLPPresent() {
        try {
            return SemanticMLPWrapper.getStandardInstance() != null;
        } catch ( Exception e ) {
            return false;
        }
    }
}
