package gov.nist.drmf.interpreter.core.grammar;

/**
 * This enum provides the translation of all Greek
 * letters.
 *
 * Definitions:
 * LaTeX:       https://de.sharelatex.com/learn/List_of_Greek_letters_and_math_symbols
 * Maple:       https://www.maplesoft.com/support/help/Maple/view.aspx?path=Greek
 * Mathematica: https://reference.wolfram.com/language/guide/GreekLetters.html
 *
 * Created by Andre Greiner-Petter on 02.11.2016.
 */
public enum GreekLetters {
    alpha("\\alpha", "alpha", "\\[Alpha]"),
    Alpha("\\Alpha", "Alpha", "\\[CapitalAlpha]");

    private String latex, maple, mathematica;

    GreekLetters( String latex, String maple, String mathematica ){
        this.latex = latex;
        this.maple = maple;
        this.mathematica = mathematica;
    }

    public static GreekLetters getLetterByLaTeX(String latex_letter){
        return null;
    }

    public static GreekLetters getLetterByMaple(String maple_letter){
        return null;
    }

    public static GreekLetters getLetterByMathematica(String mathematica_letter){
        return null;
    }
}
