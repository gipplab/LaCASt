package gov.nist.drmf.interpreter.common.letters;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This enum is a list of all greek letters in all CAS.
 * To add support for other CAS extend this list.
 *
 * HOW TO EXTEND:
 *      1) Add a new argument to each item (for instance alpha("\\alpha","alpha","\\[Alpha]", MY_NEW_CAS_SUPPORT)
 *      2) Add a variable named like your CAS to the other variables
 *      3) Implement a method to find the corresponding item @see GreekLetters#getLetterFromMaple for instance
 *      4) Extend the @see AbstractGreekLetterInterpreter for your new CAS
 *
 * LaTeX:       https://en.wikibooks.org/wiki/LaTeX/Mathematics#List_of_Mathematical_Symbols
 * Maple:       https://www.maplesoft.com/support/help/Maple/view.aspx?path=Greek
 * Mathematica: https://reference.wolfram.com/language/guide/GreekLetters.html
 *              Not includes many special characters from Mathematica like \[CapitalSampi]
 *
 * Created by Andre Greiner-Petter on 10.11.2016.
 */
public enum GreekLetters {
    // Standard forms of greek letters
    //       LaTeX           Maple       Mathematica
    alpha(  "\\alpha",      "alpha",    "\\[Alpha]"),
    Alpha(  "A",            "Alpha",    "\\[CapitalAlpha]"),
    beta(   "\\beta",       "beta",     "\\[Beta]"),
    Beta(   "B",            "Beta",     "\\[CapitalBeta]"),
    gamma(  "\\gamma",      "gamma",    "\\[Gamma]"),
    Gamma(  "\\Gamma",      "Gamma",    "\\[CapitalGamma]"),
    delta(  "\\delta",      "delta",    "\\[Delta]"),
    Delta(  "\\Delta",      "Delta",    "\\[CapitalDelta]"),
    epsilon("\\epsilon",    "epsilon",  "\\[Epsilon]"),
    Epsilon("E",            "Epsilon",  "\\[CapitalEpsilon]"),
    zeta(   "\\zeta",       "zeta",     "\\[zeta]"),
    Zeta(   "Z",            "ZETA",     "\\[CapitalZeta]"),
    eta(    "\\eta",        "eta",      "\\[Eta]"),
    Eta(    "H",            "Eta",      "\\[CapitalEta]"),
    theta(  "\\theta",      "theta",    "\\[Theta]"),
    Theta(  "\\Theta",      "Theta",    "\\[CapitalTheta]"),
    iota(   "\\iota",       "iota",     "\\[Iota]"),
    Iota(   "I",            "Iota",     "\\[CapitalIota]"),
    kappa(  "\\kappa",      "kappa",    "\\[Kappa]"),
    Kappa(  "K",            "Kappa",    "\\[CapitalKappa]"),
    lambda( "\\lambda",     "lambda",   "\\[Lambda]"),
    Lambda( "\\Lambda",     "Lambda",   "\\[CapitalLambda]"),
    mu(     "\\mu",         "mu",       "\\[Mu]"),
    Mu(     "M",            "Mu",       "\\[CapitalMu]"),
    nu(     "\\nu",         "nu",       "\\[Nu]"),
    Nu(     "N",            "Nu",       "\\[CapitalNu]"),
    xi(     "\\xi",         "xi",       "\\[Xi]"),
    Xi(     "\\Xi",         "Xi",       "\\[CapitalXi]"),
    omicron("o",            "omicron",  "\\[Omicron]"),
    Omicron("O",            "Omicron",  "\\[CapitalOmicron]"),
    pi(     "\\pi",         "pi",       "\\[Pi]"),
    Pi(     "\\Pi",         "PI",       "\\[CapitalPi]"),
    rho(    "\\rho",        "rho",      "\\[Rho]"),
    Rho(    "P",            "Rho",      "\\[CapitalRho]"),
    sigma(  "\\sigma",      "sigma",    "\\[Sigma]"),
    Sigma(  "\\Sigma",      "Sigma",    "\\[CapitalSigma]"),
    tau(    "\\tau",        "tau",      "\\[Tau]"),
    Tau(    "T",            "Tau",      "\\[CapitalTau]"),
    upsilon("\\upsilon",    "upsilon",  "\\[Upsilon]"),
    Upsilon("\\Upsilon",    "Upsilon",  "\\[CapitalUpsilon]"),
    phi(    "\\phi",        "phi",      "\\[Phi]"),
    Phi(    "\\Phi",        "Phi",      "\\[CapitalPhi]"),
    chi(    "\\chi",        "chi",      "\\[Chi]"),
    Chi(    "X",            "CHI",      "\\[CapitalChi]"),
    psi(    "\\psi",        "psi",      "\\[Psi]"),
    Psi(    "\\Psi",        "Psi",      "\\[CapitalPsi]"),
    omega(  "\\omega",      "omega",    "\\[Omega]"),
    Omega(  "\\Omega",      "Omega",    "\\[CapitalOmega]"),

    // variant forms of greek letters
    // varrho is not in the list of supported greek letters in maple, but it works in Maple 2015
    //              LaTeX               Maple           Mathematica
    varepsilon( "\\varepsilon",     "varepsilon",   "\\[CurlyEpsilon]"),
    vartheta(   "\\vartheta",       "vartheta",     "\\[CurlyTheta]"),
    varkappa(   "\\varkappa",       "varkappa",     "\\[CurlyKappa]"),
    varpi(      "\\varpi",          "varpi",        "\\[CurlyPi]"),
    varrho(     "\\varrho",         "varrho",       "\\[CurlyRho]"),
    varsigma(   "\\varsigma",       "varsigma",     "\\[FinalSigma]"),
    varUpsilon( null,               null,           "\\[CurlyCapitalUpsilon]"),
    varphi(     "\\varphi",         "varphi",       "\\[CurlyPhi]"),
    defaultValue("","","");

    // names are final (not changeable) and public for fast access
    public final String
            latex,
            maple,
            mathematica;

    private static final int IDX_VARIANT = 23;

    /**
     * Constructs an enum item
     * @param latex latex letter
     * @param maple maple letter
     * @param mathematica mathematica letter
     */
    GreekLetters(String latex, String maple, String mathematica){
        this.latex = latex;
        this.maple = maple;
        this.mathematica = mathematica;
    }

    // regex for special cases in LaTeX
    private static final String PATTERN_SPECIAL_LATEX =
            "[ABEZHIKMNoOPTX]";

    private static final String PATTERN_SPECIAL_MAPLE =
            "ZETA|PI|CHI";

    // regex for variant mathematica strings (without \[ on start)
    private static final String PATTERN_VARIANT_MATHEMATICA =
            "(Curly|Final).+";

    // pattern for mathematica (without \[ on start)
    private static final Pattern MATHEMATICA_NUDE_PATTERN =
            Pattern.compile("(Capital)?(.+)");

    /**
     * Returns the GreekLetters item by given latex greek letter
     * @param latex greek letter in LaTeX
     * @return item represents the greek letter
     */
    public static GreekLetters getLetterFromLaTeX(String latex){
        if ( latex.matches(PATTERN_SPECIAL_LATEX) ){
            for ( GreekLetters l : GreekLetters.values() )
                if ( l.latex.equals(latex) )
                    return l;
            return defaultValue;
        }
        // the item name is the latex string without \
        else return GreekLetters.valueOf( latex.substring(1) );
    }

    /**
     * Returns the GreekLetters item by given Maple greek letter
     * @param maple greek letter in Maple
     * @return item represents the greek letter
     */
    public static GreekLetters getLetterFromMaple(String maple){
        if ( maple.matches( PATTERN_SPECIAL_MAPLE )){
            // special case are 3 capital letters
            //  ZETA -> Zeta; PI -> Pi; CHI -> Chi
            String letter_name = maple.charAt(0) + maple.substring(1).toLowerCase();
            return GreekLetters.valueOf(letter_name);
        }
        // the item name is simply the maple string
        return GreekLetters.valueOf(maple);
    }

    /**
     * Returns the GreekLetters item by given Mathematica greek letter
     * @param mathematica greek letter in Mathematica
     * @return item represents the greek letter
     */
    public static GreekLetters getLetterFromMathematica(String mathematica){
        // delete \[ and ]
        String nude = mathematica.substring(2, mathematica.length()-1);
        if ( nude.matches( PATTERN_VARIANT_MATHEMATICA ) ){
            GreekLetters[] l = GreekLetters.values();
            for ( int i = IDX_VARIANT; i < l.length; i++ )
                if ( l[i].mathematica.equals(mathematica) )
                    return l[i];
        } else {
            Matcher m = MATHEMATICA_NUDE_PATTERN.matcher(nude);
            if ( !m.matches() ) return defaultValue;
            // if the 2nd group is null, there is no "Capital" in front of the letter
            // that means it has to be lower case letter.
            if ( m.group(1) == null ) return GreekLetters.valueOf( m.group(2).toLowerCase() );
            // otherwise it is a capital letter
            else return GreekLetters.valueOf( m.group(2) );
        }
        return defaultValue;
    }
}
