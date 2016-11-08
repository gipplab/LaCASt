package gov.nist.drmf.interpreter.common;

/**
 * An enum of all greek letters out there.
 *
 * LaTeX:       https://de.sharelatex.com/learn/List_of_Greek_letters_and_math_symbols
 * Maple:       https://www.maplesoft.com/support/help/Maple/view.aspx?path=Greek
 * Mathematica: https://reference.wolfram.com/language/guide/GreekLetters.html
 *
 * TODO this is only for bijective translations... VariantUpsilon(null, null, "\[CurlyCapitalUpsilon]")
 *
 * Created by Andre Greiner-Petter on 02.11.2016.
 */
public enum GreekLetters {
    // some standard test cases
    alpha("\\alpha","alpha","\\[Alpha]"),
    Alpha("A","Alpha","\\[CapitalAlpha]"),
    Beta("B","Beta","\\[CapitalBeta]"),
    beta("\\beta","beta","\\[Beta]"),
    gamma("\\gamma","gamma","\\[Gamma]"),
    Gamma("\\Gamma","Gamma","\\[CapitalGamma]"),

    varepsilon("\\varepsilon","varepsilon","\\[CurlyEpsilon]"),
    varphi("\\varphi","varphi","\\[CurlyPhi]"),

    Zeta("Z","Zeta","\\[CapitalZeta]"),
    Eta("H","Eta","\\[CapitalEta]"),
    Iota("I","Iota","\\[CapitalIota]"),
    Kappa("K","Kappa","\\[CapitalKappa]"),
    Mu("M","Mu","\\[CapitalMu]"),
    Nu("N","Nu","\\[CapitalNu]"),
    omicron("o","omicron","\\[Omicron]"),
    Omicron("O","Omicron","\\[CapitalOmicron]"),
    Pi("\\Pi","PI","\\[CapitalPi]"),
    Rho("P","Rho","\\[CapitalRho]"),
    Tau("T","Tau","\\[CapitalTau]"),
    Chi("X","Chi","\\[CapitalChi]");

    // special variant forms
    // TODO this is only for bijective translations
    //varsigma("\\varsigma","varsigma","\\[FinalSigma]");

    // greek letter in latex, maple and mathematica
    public String latex, maple, mathematica;

    /**
     * Creates a TestLetters object
     * @param latex
     * @param maple
     * @param mathematica
     */
    GreekLetters(String latex, String maple, String mathematica){
        this.latex = latex;
        this.maple = maple;
        this.mathematica = mathematica;
    }
}
