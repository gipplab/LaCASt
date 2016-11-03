package gov.nist.drmf.core.grammar.tests;

import gov.nist.drmf.interpreter.core.grammar.GreekLetters;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

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
    /**
     * This enumeration defines all test cases.
     *      Few examples of easy translation
     *      Few examples of variant forms of greek letters
     *      All cases of special cases (not defined in LaTeX)
     */
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

        // greek letter in latex, maple and mathematica
        private String latex, maple, mathematica;

        /**
         * Creates a TestLetters object
         * @param latex
         * @param maple
         * @param mathematica
         */
        TestLetters(String latex, String maple, String mathematica){
            this.latex = latex;
            this.maple = maple;
            this.mathematica = mathematica;
        }
    }

    /**
     * Test LaTeX to Maple for all test cases
     * @return
     */
    @TestFactory
    Iterable<DynamicTest> latexToMapleTests(){
        List<DynamicTest> list = new LinkedList();
        for ( TestLetters test : TestLetters.values() ){
            list.add( generalTest("Test-LaTeX2Maple",test.latex,test.maple) );
        }
        return list;
    }

    /**
     * Test LaTeX to Mathematica for all test cases
     * @return
     */
    @TestFactory
    Iterable<DynamicTest> latexToMathematicaTests(){
        List<DynamicTest> list = new LinkedList();
        for ( TestLetters test : TestLetters.values() ){
            list.add( generalTest("Test-LaTeX2Mathematica",test.latex,test.mathematica) );
        }
        return list;
    }

    /**
     * Test Maple to LaTeX for all test cases
     * @return
     */
    @TestFactory
    Iterable<DynamicTest> mapleToLaTeXTests(){
        List<DynamicTest> list = new LinkedList();
        for ( TestLetters test : TestLetters.values() ){
            list.add( generalTest("Test-Maple2LaTeX",test.maple,test.latex) );
        }
        return list;
    }

    /**
     * Test Maple to Mathematica for all test cases
     * @return
     */
    @TestFactory
    Iterable<DynamicTest> mapleToMathematicaTests(){
        List<DynamicTest> list = new LinkedList();
        for ( TestLetters test : TestLetters.values() ){
            list.add( generalTest("Test-Maple2Mathematica",test.maple,test.mathematica) );
        }
        return list;
    }

    /**
     * Test Mathematica to LaTeX for all test cases
     * @return
     */
    @TestFactory
    Iterable<DynamicTest> mathematicaToLaTeXTests(){
        List<DynamicTest> list = new LinkedList();
        for ( TestLetters test : TestLetters.values() ){
            list.add( generalTest("Test-Mathematica2LaTeX",test.mathematica,test.latex) );
        }
        return list;
    }

    /**
     * Test Mathematica to Maple for all test cases
     * @return
     */
    @TestFactory
    Iterable<DynamicTest> mathematicaToMapleTests(){
        List<DynamicTest> list = new LinkedList();
        for ( TestLetters test : TestLetters.values() ){
            list.add( generalTest("Test-Mathematica2Maple",test.mathematica,test.maple) );
        }
        return list;
    }

    /**
     * Generate a test for given strings.
     * @param note  name of the test
     * @param from  string representation that will translated
     * @param to    what we expected after the translation finished
     * @return a test class for given parameters
     */
    private DynamicTest generalTest(String note, String from, String to){
        return dynamicTest(
                note + ": " + from,
                () -> assertEquals(
                        to,
                        GreekLetters.convertTexToMaple(from)
                )
        );
    }
}
