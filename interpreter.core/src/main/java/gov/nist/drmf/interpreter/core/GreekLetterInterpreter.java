package gov.nist.drmf.interpreter.core;

/**
 * This class provides the translation of all Greek
 * letters from and to CAS and LaTeX.
 *
 * LaTeX:       https://de.sharelatex.com/learn/List_of_Greek_letters_and_math_symbols
 * Maple:       https://www.maplesoft.com/support/help/Maple/view.aspx?path=Greek
 * Mathematica: https://reference.wolfram.com/language/guide/GreekLetters.html
 *
 * TODO this is only for bijective translations... VariantUpsilon(null, null, "\[CurlyCapitalUpsilon]")
 *
 * Created by Andre Greiner-Petter on 02.11.2016.
 */
public abstract class GreekLetterInterpreter {
    // The backslash character
    public static final char BACK_SLASH = '\\';

    // capital letters in mathematica are written with \[Capital...]
    public static final String MATHEMATICA_CAPITAL = "Capital";

    // All variant letters in Maple starts with var...
    public static final String VARIANT_MAPLE_START = "var";

    /**
     * All variant mathematica letters starts with \[Curly...]
     * excepts variant sigma which is \[FinalSigma]
     */
    public static final String VARIANT_MATHEMATICA_START = "Curly";

    /**
     * This enumeration contains all special cases for conversion.
     * For instance a capital Alpha doesn't exist in LaTeX, it's
     * just an A.
     */
    @SuppressWarnings("unused")
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

        // all representations
        String latex, maple, mathematica;

        SpecialCases(String latex, String maple, String mathematica){
            this.latex = latex;
            this.maple = maple;
            this.mathematica = mathematica;
        }

        /**
         * Gets the specified special letter from a given latex letter
         * @param latex for instance A returns the special case Alpha
         * @return special case with given representation in latex
         */
        public static SpecialCases getSpecialFromLatex(String latex){
            for (SpecialCases l : SpecialCases.values())
                if ( l.latex.matches(latex) ) return l;
            return null;
        }

        /**
         * The same like <getSpecialFromLatex> but for maple
         * @param maple string of special case
         * @return special case
         */
        public static SpecialCases getSpecialFromMaple(String maple){
            for (SpecialCases l : SpecialCases.values())
                if ( l.maple.matches(maple) ) return l;
            return null;
        }

        /**
         * The same like <getSpecialFromLatex> but for mathematica
         * @param mathematica string of special case
         * @return special case
         */
        public static SpecialCases getSpecialFromMathematica(String mathematica){
            mathematica = mathematica.substring(2);
            if ( mathematica.startsWith(MATHEMATICA_CAPITAL) ) {
                mathematica = mathematica.split(MATHEMATICA_CAPITAL)[1];
                for (SpecialCases l : SpecialCases.values())
                    if ( l.mathematica.substring(2+MATHEMATICA_CAPITAL.length()).startsWith(mathematica) ) return l;
                return null;
            } else return omicron;
        }
    }

    // Pattern for all special cases in latex
    public static final String SPECIAL_LATEX_PATTERN =
            "[ABZHIKMNoOPTX]";

    // Pattern for all special cases in Maple
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
     * All variant variables has the form \[Curly<varName>] except
     * the variant form of Sigma. This is \[FinalSigma]
     */
    public static final String VARIANT_MATHEMATICA_PATTERN =
            "\\\\\\[(Curly|Final).+";

    /**
     * All variant letters in Maple starts with var...
     */
    public static final String VARIANT_MAPLE_PATTERN =
            "var.+";

    /**
     * Flips the first character of given string to upper case.
     * @param s string
     * @return s with upper case starting character
     */
    private static String firstCharacterToUpper(String s){
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /**
     * Converts a given greek letter in latex to maple
     * @param latex_letter greek letter in latex
     * @return greek letter in maple
     */
    public static String convertTexToMaple(String latex_letter){
        if (latex_letter.matches(SPECIAL_LATEX_PATTERN)){
            return SpecialCases.getSpecialFromLatex(latex_letter).maple;
        }
        return latex_letter.substring(1);
    }

    /**
     * Converts a given greek letter in latex to mathematica
     * @param latex_letter greek letter in latex
     * @return greek letter in mathematica
     */
    public static String convertTexToMathematica(String latex_letter){
        return convertMapleToMathematica(convertTexToMaple(latex_letter));
    }

    /**
     * Converts a given greek letter in maple to latex
     * @param maple_letter greek letter in maple
     * @return greek letter in latex
     */
    public static String convertMapleToTex(String maple_letter){
        if (maple_letter.matches(SPECIAL_MAPLE_PATTERN)){
            return SpecialCases.getSpecialFromMaple(maple_letter).latex;
        }
        return BACK_SLASH + maple_letter;
    }

    /**
     * Converts a given greek letter in maple to mathematica
     * @param maple_letter greek letter in maple
     * @return greek letter in mathematica
     */
    public static String convertMapleToMathematica(String maple_letter){
        if ( maple_letter.matches(VARIANT_MAPLE_PATTERN) ){
            if ( maple_letter.endsWith("sigma") ) return "\\[FinalSigma]";
            String math = firstCharacterToUpper(maple_letter.substring(3));
            return "\\[" + VARIANT_MATHEMATICA_START + math + "]";
        } else if ( maple_letter.matches("[A-Z].+") ){
            maple_letter = MATHEMATICA_CAPITAL + maple_letter;
        } else if ( maple_letter.matches("[a-z].+") ){
            maple_letter = firstCharacterToUpper(maple_letter);
        } else return null; // input was not a greek letter in Maple
        return BACK_SLASH + "[" + maple_letter + "]";
    }

    /**
     * Converts a given greek letter in mathematica to latex
     * @param mathematica_letter greek letter in mathematica
     * @return greek letter in latex
     */
    public static String convertMathematicaToTex(String mathematica_letter){
        if ( mathematica_letter.matches(SPECIAL_MATHEMATICA_PATTERN) ){
            return SpecialCases.getSpecialFromMathematica(mathematica_letter).latex;
        } else if ( mathematica_letter.matches(VARIANT_MATHEMATICA_PATTERN) ){
            return convertVariantMathematicaToTex(mathematica_letter);
        }
        return BACK_SLASH + convertMathematicaToMaple(mathematica_letter);
    }

    /**
     * Converts a given greek letter in mathematica to maple
     * @param mathematica_letter greek letter in mathematica
     * @return greek letter in maple
     */
    public static String convertMathematicaToMaple( String mathematica_letter ){
        if ( mathematica_letter.matches(VARIANT_MATHEMATICA_PATTERN) ) {
            return convertVariantMathematicaToMaple(mathematica_letter);
        }

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

    /**
     * Converts a given variant greek letter in mathematica to maple
     * @param variant_mathematica variant greek letter in mathematica
     * @return variant greek letter in maple
     */
    private static String convertVariantMathematicaToMaple( String variant_mathematica ){
        // luckly "Final" and "Curly" has the same length...
        return VARIANT_MAPLE_START +
                variant_mathematica.substring(
                        2+VARIANT_MATHEMATICA_START.length(),
                        variant_mathematica.length()-1)
                        .toLowerCase();
    }

    /**
     * Converts a given variant greek letter in mathematica to latex
     * @param variant_mathematica variant greek letter in mathematica
     * @return variant greek letter in latex
     */
    private static String convertVariantMathematicaToTex( String variant_mathematica ){
        return BACK_SLASH + convertVariantMathematicaToMaple(variant_mathematica);
    }
}
