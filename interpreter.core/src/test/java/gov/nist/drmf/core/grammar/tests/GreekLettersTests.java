package gov.nist.drmf.core.grammar.tests;

import gov.nist.drmf.interpreter.core.grammar.GreekLetters;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test conversion of greek letters.
 *
 * LaTeX:       https://de.sharelatex.com/learn/List_of_Greek_letters_and_math_symbols
 * Maple:       https://www.maplesoft.com/support/help/Maple/view.aspx?path=Greek
 * Mathematica: https://reference.wolfram.com/language/guide/GreekLetters.html
 *
 * Created by Andre Greiner-Petter on 02.11.2016.
 */
public class GreekLettersTests {
    private enum TestLetters{
        // some standard test cases
        alpha("\\alpha","alpha","\\[Alpha]"),
        beta("\\beta","beta","\\[Beta]"),
        gamma("\\gamma","gamma","\\[Gamma]"),
        Gamma("\\Gamma","Gamma","\\[CapitalGamma]"),

        // variant forms
        varepsilon("\\varepsilon","varepsilon","\\[CurlyEpsilon]"),
        varphi("\\varphi","varphi","\\[CurlyPhi]"),

        // special cases
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

        // special variant forms
        // TODO this is only for bijective translations
        //varsigma("\\varsigma","varsigma","\\[FinalSigma]");

        private String latex, maple, mathematica;

        TestLetters(String latex, String maple, String mathematica){
            this.latex = latex;
            this.maple = maple;
            this.mathematica = mathematica;
        }

        public static final int NUM_STANDARD = 4;
        public static final int NUM_VAR = 2;
        public static final int NUM_SPECIAL = 13;
    }

    @Test
    void patternTest(){
        String pattern = "\\" + '[' + "(Capital|)[ABZHIKMNoOPTX].+";
        String test = "\\[Alpha]";
        System.out.println(test.matches(pattern));
        // TODO why the hack is this false?
    }

    // TODO parameterized tests

    void generalTest(int i){
        TestLetters[] tests = TestLetters.values();
        assertEquals(
                tests[i].maple,
                GreekLetters.convertTexToMaple(tests[i].latex),
                "Failed to convert " + tests[i].latex + " to Maple.");
        assertEquals(
                tests[i].mathematica,
                GreekLetters.convertTexToMathematica(tests[i].latex),
                "Failed to convert " + tests[i].latex + " to Mathematica.");
        assertEquals(
                tests[i].latex,
                GreekLetters.convertMapleToTex(tests[i].maple),
                "Failed to convert " + tests[i].maple + " to LaTeX.");
        assertEquals(
                tests[i].mathematica,
                GreekLetters.convertMapleToMathematica(tests[i].maple),
                "Failed to convert " + tests[i].maple + " to Mathematica.");
        assertEquals(
                tests[i].latex,
                GreekLetters.convertMathematicaToTex(tests[i].mathematica),
                "Failed to convert " + tests[i].mathematica + " to LaTeX.");
        assertEquals(
                tests[i].maple,
                GreekLetters.convertMathematicaToMaple(tests[i].mathematica),
                "Failed to convert " + tests[i].mathematica + " to Maple.");
    }
}
