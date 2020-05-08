package gov.nist.drmf.interpreter.common;

/**
 * This enum is a list of all greek symbols in all CAS.
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
public enum GreekLetterTestCases {
    // Standard forms of greek symbols
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

    // variant forms of greek symbols
    // varrho is not in the list of supported greek symbols in maple, but it works in Maple 2015
    //              LaTeX               Maple           Mathematica
    varepsilon( "\\varepsilon",     "varepsilon",   "\\[CurlyEpsilon]"),
    vartheta(   "\\vartheta",       "vartheta",     "\\[CurlyTheta]"),
    varkappa(   "\\varkappa",       "varkappa",     "\\[CurlyKappa]"),
    varpi(      "\\varpi",          "varpi",        "\\[CurlyPi]"),
    varrho(     "\\varrho",         "varrho",       "\\[CurlyRho]"),
    varsigma(   "\\varsigma",       "varsigma",     "\\[FinalSigma]"),
    varUpsilon( null,               null,           "\\[CurlyCapitalUpsilon]"),
    varphi(     "\\varphi",         "varphi",       "\\[CurlyPhi]");

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
    GreekLetterTestCases(String latex, String maple, String mathematica){
        this.latex = latex;
        this.maple = maple;
        this.mathematica = mathematica;
    }
}
