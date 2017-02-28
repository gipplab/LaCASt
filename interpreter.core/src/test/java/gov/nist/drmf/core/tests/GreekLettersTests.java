package gov.nist.drmf.core.tests;

import gov.nist.drmf.interpreter.common.Keys;
import gov.nist.drmf.interpreter.common.symbols.GreekLetters;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

/**
 * Created by Andre Greiner-Petter on 10.11.2016.
 */
public class GreekLettersTests {
    private static GreekLetters g;

    @BeforeAll
    static void init(){
        g = new GreekLetters("","");
        g.init();
    }

    /**
     * Test LaTeX to Maple for all test cases
     * @return
     */
    @TestFactory
    Iterable<DynamicTest> latexToMapleTests(){
        List<DynamicTest> list = new LinkedList();
        for ( GreekLetterTestCases test : GreekLetterTestCases.values() ){
            if ( test.latex == null ) continue;
            list.add( dynamicTest(
                    "LaTeX->Maple" + ": " + test.latex,
                    () -> assertEquals(
                            test.maple,
                            g.translate(Keys.KEY_LATEX, Keys.KEY_MAPLE, test.latex)
                    )
                    )
            );
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
        for ( GreekLetterTestCases test : GreekLetterTestCases.values() ){
            if ( test.latex == null ) continue;
            list.add( dynamicTest(
                    "LaTeX->Mathematica" + ": " + test.latex,
                    () -> assertEquals(
                            test.mathematica,
                            g.translate(Keys.KEY_LATEX, Keys.KEY_MATHEMATICA, test.latex)
                    )
                    )
            );
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
        for ( GreekLetterTestCases test : GreekLetterTestCases.values() ){
            if ( test.maple == null ) continue;
            list.add( dynamicTest(
                    "Maple->LaTeX" + ": " + test.maple,
                    () -> assertEquals(
                            test.latex,
                            g.translate(Keys.KEY_MAPLE, Keys.KEY_LATEX, test.maple)
                    )
                    )
            );
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
        for ( GreekLetterTestCases test : GreekLetterTestCases.values() ){
            if ( test.mathematica == null ) continue;
            list.add( dynamicTest(
                    "Mathematica->LaTeX" + ": " + test.mathematica,
                    () -> assertEquals(
                            test.latex,
                            g.translate(Keys.KEY_MATHEMATICA, Keys.KEY_LATEX, test.mathematica)
                    )
                    )
            );
        }
        return list;
    }
}
