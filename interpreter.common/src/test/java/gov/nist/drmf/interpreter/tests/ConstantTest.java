package gov.nist.drmf.interpreter.tests;

import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.symbols.Constants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import static junit.framework.TestCase.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

/**
 * @author Andre Greiner-Petter
 */
public class ConstantTest {
    private static Constants g;

    @BeforeAll
    static void init(){
        g = new Constants("","");
        try { g.init(); }
        catch ( IOException ioe ){
            System.err.println(ioe.getMessage());
            ioe.printStackTrace();
            fail("Exception during initialization.");
        }
    }

    /**
     * Test dlmf to Maple for all test cases
     * @return
     */
    @TestFactory
    Iterable<DynamicTest> dlmfToMapleTests(){
        List<DynamicTest> list = new LinkedList();
        for ( ConstantTestCases test : ConstantTestCases.values() ){
            if ( test.dlmf == null ) continue;
            list.add( dynamicTest(
                    "dlmf->Maple" + ": " + test.dlmf,
                    () -> assertEquals(
                            test.maple,
                            g.translate(Keys.KEY_DLMF, Keys.KEY_MAPLE, test.dlmf)
                    )
                    )
            );
        }
        return list;
    }

    /**
     * Test dlmf to Mathematica for all test cases
     * @return
     */
    @TestFactory
    Iterable<DynamicTest> dlmfToMathematicaTests(){
        List<DynamicTest> list = new LinkedList();
        for ( ConstantTestCases test : ConstantTestCases.values() ){
            if ( test.dlmf == null ) continue;
            list.add( dynamicTest(
                    "dlmf->Mathematica" + ": " + test.dlmf,
                    () -> assertEquals(
                            test.mathematica,
                            g.translate(Keys.KEY_DLMF, Keys.KEY_MATHEMATICA, test.dlmf)
                    )
                    )
            );
        }
        return list;
    }

    /**
     * Test Maple to dlmf for all test cases
     * @return
     */
    @TestFactory
    Iterable<DynamicTest> mapleTodlmfTests(){
        List<DynamicTest> list = new LinkedList();
        for ( ConstantTestCases test : ConstantTestCases.values() ){
            if ( test.maple == null ) continue;
            list.add( dynamicTest(
                    "Maple->dlmf" + ": " + test.maple,
                    () -> assertEquals(
                            test.dlmf,
                            g.translate(Keys.KEY_MAPLE, Keys.KEY_DLMF, test.maple)
                    )
                    )
            );
        }
        return list;
    }

    /**
     * Test Mathematica to dlmf for all test cases
     * @return
     */
    @TestFactory
    Iterable<DynamicTest> mathematicaTodlmfTests(){
        List<DynamicTest> list = new LinkedList();
        for ( ConstantTestCases test : ConstantTestCases.values() ){
            if ( test.mathematica == null ) continue;
            list.add( dynamicTest(
                    "Mathematica->dlmf" + ": " + test.mathematica,
                    () -> assertEquals(
                            test.dlmf,
                            g.translate(Keys.KEY_MATHEMATICA, Keys.KEY_DLMF, test.mathematica)
                    )
                    )
            );
        }
        return list;
    }
}
