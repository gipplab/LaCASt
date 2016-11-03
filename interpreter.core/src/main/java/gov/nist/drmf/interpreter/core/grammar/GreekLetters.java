package gov.nist.drmf.interpreter.core.grammar;

/**
 * This class provides the translation of all Greek
 * letters from and to CAS and LaTeX.
 *
 * LaTeX:       https://de.sharelatex.com/learn/List_of_Greek_letters_and_math_symbols
 * Maple:       https://www.maplesoft.com/support/help/Maple/view.aspx?path=Greek
 * Mathematica: https://reference.wolfram.com/language/guide/GreekLetters.html
 *
 * TODO this is only for bijective translations...
 *
 * Created by Andre Greiner-Petter on 02.11.2016.
 */
public abstract class GreekLetters {
    public static final char BACK_SLASH = '\\';

    public static final String MATHEMATICA_CAPITAL = "Capital";

    private enum SpecialCases{
        Alpha("A","Alpha","\\[CapitalAlpha]"),
        Beta("B","Beta","\\[CapitalBeta]"),
        Zeta("Z","Zeta","\\[CapitalZeta]"),
        Eta("H","Eta","\\[CapitalEta]"),
        Iota("I","Iota","\\[CapitalIota]"),
        Kappa("K","Kappa","\\[CapitalKappa]"),
        Mu("M","Mu","\\[CapitalMu]"),
        Nu("N","Nu","\\[CapitalNu]"),
        omicron("o","omicron","\\[Omicron]"),
        Omicron("O","Omicron","\\[CapitalOmicron]"),
        Rho("P","Rho","\\[CapitalRho]"),
        Tau("T","Tau","\\[CapitalTau]"),
        Chi("X","Chi","\\[CapitalChi]");

        String latex, maple, mathematica;
        String id;

        SpecialCases(String latex, String maple, String mathematica){
            this.id = latex;
            this.latex = latex;
            this.maple = maple;
            this.mathematica = mathematica;
        }

        public static SpecialCases getSpecialFromLatex(String latex){
            for (SpecialCases l : SpecialCases.values())
                if ( l.latex.matches(latex) ) return l;
            return null;
        }

        public static SpecialCases getSpecialFromMaple(String maple){
            for (SpecialCases l : SpecialCases.values())
                if ( l.maple.matches(maple) ) return l;
            return null;
        }

        public static SpecialCases getSpecialFromMathematica(String mathematica){
            mathematica = mathematica.substring(2);
            if ( mathematica.startsWith(MATHEMATICA_CAPITAL) ) {
                mathematica = mathematica.split(MATHEMATICA_CAPITAL)[1];
                for (SpecialCases l : SpecialCases.values())
                    if ( l.mathematica.substring(2).startsWith(mathematica) ) return l;
                return null;
            } else return omicron;
        }
    }

    public static final String SPECIAL_LATEX_PATTERN =
            "[ABZHIKMNoOPTX]";

    public static final String SPECIAL_MAPLE_PATTERN =
            "[ABZEIKMNoORTC].+";

    /**
     * I simply don't like regex...
     * To match "[" you need the following
     *      \\\\[
     * Since Java needs \\ to represent a \ in regex
     * and regex needs \[ since [ is a special character
     * in regex, we need to get \[ after Java representation.
     * Which brings us to \\\\[ because
     *      \\\\[   -> is a java String looks like: \\[
     *      \\[     -> is a regex representation for \[
     *      \[      -> is exactly what we are looking for
     */
    public static final String SPECIAL_MATHEMATICA_PATTERN =
            "\\\\\\[(Capital[ABZEIKMNoORTC]|O).+";

    /**
     * Flips the first character of given string to upper case.
     * @param s string
     * @return s with upper case starting character
     */
    private static String firstCharacterToUpper(String s){
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    public static String convertTexToMaple(String latex_letter){
        if (latex_letter.matches(SPECIAL_LATEX_PATTERN)){
            return SpecialCases.getSpecialFromLatex(latex_letter).maple;
        }
        return latex_letter.substring(1);
    }

    public static String convertTexToMathematica(String latex_letter){
        return convertMapleToMathematica(convertTexToMaple(latex_letter));
    }

    public static String convertMapleToTex(String maple_letter){
        if (maple_letter.matches(SPECIAL_MAPLE_PATTERN)){
            return SpecialCases.getSpecialFromMaple(maple_letter).latex;
        }
        return BACK_SLASH + maple_letter;
    }

    public static String convertMapleToMathematica(String maple_letter){
        if ( maple_letter.matches("[A-Z].+") ){
            maple_letter = MATHEMATICA_CAPITAL + maple_letter;
        } else if ( maple_letter.matches("[a-z].+") ){
            maple_letter = firstCharacterToUpper(maple_letter);
        } else return null; // input was not a greek letter in Maple
        return BACK_SLASH + "[" + maple_letter + "]";
    }

    public static String convertMathematicaToTex(String mathematica_letter){
        if ( mathematica_letter.matches(SPECIAL_MATHEMATICA_PATTERN) ){
            return SpecialCases.getSpecialFromMathematica(mathematica_letter).latex;
        }
        return BACK_SLASH + convertMathematicaToMaple(mathematica_letter);
    }

    public static String convertMathematicaToMaple( String mathematica_letter ){
        // delete \\[ and ]
        mathematica_letter = mathematica_letter.substring(2, mathematica_letter.length()-1);

        // Either "Capital<GreekLetter>" or "<GreekLetter>" while <GreekLetter> begins
        // always with a capital letter
        // first case: capital greek letter
        if ( mathematica_letter.startsWith(MATHEMATICA_CAPITAL) ) {
            return mathematica_letter.substring(MATHEMATICA_CAPITAL.length());
        } else { // second case: non-capital greek letter
            return mathematica_letter.toLowerCase();
        }
    }
}
