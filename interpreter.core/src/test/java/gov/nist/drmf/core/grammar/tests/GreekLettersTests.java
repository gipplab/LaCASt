package gov.nist.drmf.core.grammar.tests;

import gov.nist.drmf.interpreter.common.GreekLetters;
import gov.nist.drmf.interpreter.core.GreekLetterInterpreter;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

/**
 * Test conversion of greek letters. Take a look to
 *      gov.nist.drmf.interpreter.common.GreekLetters.java
 *
 * Created by Andre Greiner-Petter on 02.11.2016.
 */
public class GreekLettersTests {
    /**
     * Test LaTeX to Maple for all test cases
     * @return
     */
    @TestFactory
    Iterable<DynamicTest> latexToMapleTests(){
        List<DynamicTest> list = new LinkedList();
        for ( GreekLetters test : GreekLetters.values() ){
            list.add( dynamicTest(
                    "LaTeX->Maple" + ": " + test.latex,
                    () -> assertEquals(
                            test.maple,
                            GreekLetterInterpreter.convertTexToMaple(test.latex)
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
        for ( GreekLetters test : GreekLetters.values() ){
            list.add( dynamicTest(
                    "LaTeX->Mathematica" + ": " + test.latex,
                    () -> assertEquals(
                            test.mathematica,
                            GreekLetterInterpreter.convertTexToMathematica(test.latex)
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
        for ( GreekLetters test : GreekLetters.values() ){
            list.add( dynamicTest(
                    "Maple->LaTeX" + ": " + test.maple,
                    () -> assertEquals(
                            test.latex,
                            GreekLetterInterpreter.convertMapleToTex(test.maple)
                    )
                    )
            );
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
        for ( GreekLetters test : GreekLetters.values() ){
            list.add( dynamicTest(
                    "Maple->Mathematica" + ": " + test.maple,
                    () -> assertEquals(
                            test.mathematica,
                            GreekLetterInterpreter.convertMapleToMathematica(test.maple)
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
        for ( GreekLetters test : GreekLetters.values() ){
            list.add( dynamicTest(
                    "Mathematica->LaTeX" + ": " + test.mathematica,
                    () -> assertEquals(
                            test.latex,
                            GreekLetterInterpreter.convertMathematicaToTex(test.mathematica)
                    )
                    )
            );
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
        for ( GreekLetters test : GreekLetters.values() ){
            list.add( dynamicTest(
                    "Mathematica->Maple" + ": " + test.mathematica,
                    () -> assertEquals(
                            test.maple,
                            GreekLetterInterpreter.convertMathematicaToMaple(test.mathematica)
                    )
                    )
            );
        }
        return list;
    }
}
