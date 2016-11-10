package gov.nist.drmf.core.tests;

import gov.nist.drmf.interpreter.common.letters.GreekLetters;
import gov.nist.drmf.interpreter.common.letters.AbstractGreekLetterInterpreter;
import gov.nist.drmf.interpreter.core.grammar.MapleGreekLetterInterpreter;
import gov.nist.drmf.interpreter.core.grammar.MathematicaGreekLetterInterpreter;
import org.junit.jupiter.api.BeforeAll;
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
    private static AbstractGreekLetterInterpreter
            mathematicaInt,
            mapleInt;

    @BeforeAll
    public static void init(){
        mathematicaInt = new MathematicaGreekLetterInterpreter();
        mapleInt = new MapleGreekLetterInterpreter();
    }

    /**
     * Test LaTeX to Maple for all test cases
     * @return
     */
    @TestFactory
    Iterable<DynamicTest> latexToMapleTests(){
        List<DynamicTest> list = new LinkedList();
        for ( GreekLetters test : GreekLetters.values() ){
            if ( test.latex == null || test.equals(GreekLetters.defaultValue)) continue;
            list.add( dynamicTest(
                    "LaTeX->Maple" + ": " + test.latex,
                    () -> assertEquals(
                            test.maple,
                            mapleInt.convertToCAS(test.latex)
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
            if ( test.latex == null || test.equals(GreekLetters.defaultValue) ) continue;
            list.add( dynamicTest(
                    "LaTeX->Mathematica" + ": " + test.latex,
                    () -> assertEquals(
                            test.mathematica,
                            mathematicaInt.convertToCAS(test.latex)
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
            if ( test.maple == null || test.equals(GreekLetters.defaultValue) ) continue;
            list.add( dynamicTest(
                    "Maple->LaTeX" + ": " + test.maple,
                    () -> assertEquals(
                            test.latex,
                            mapleInt.convertFromCAS(test.maple)
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
            if ( test.mathematica == null || test.equals(GreekLetters.defaultValue) ) continue;
            list.add( dynamicTest(
                    "Mathematica->LaTeX" + ": " + test.mathematica,
                    () -> assertEquals(
                            test.latex,
                            mathematicaInt.convertFromCAS(test.mathematica)
                    )
                    )
            );
        }
        return list;
    }
}
