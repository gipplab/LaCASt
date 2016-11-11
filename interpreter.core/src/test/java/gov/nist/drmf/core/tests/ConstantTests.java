package gov.nist.drmf.core.tests;

import gov.nist.drmf.interpreter.common.GlobalConstants;
import gov.nist.drmf.interpreter.common.letters.Constants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

/**
 * @author Andre Greiner-Petter
 */
public class ConstantTests {
    private static Constants g;

    @BeforeAll
    static void init(){
        g = Constants.getConstantsInstance();
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
                            g.translate(GlobalConstants.KEY_DLMF, GlobalConstants.KEY_MAPLE, test.dlmf)
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
                            g.translate(GlobalConstants.KEY_DLMF, GlobalConstants.KEY_MATHEMATICA, test.dlmf)
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
                            g.translate(GlobalConstants.KEY_MAPLE, GlobalConstants.KEY_DLMF, test.maple)
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
                            g.translate(GlobalConstants.KEY_MATHEMATICA, GlobalConstants.KEY_DLMF, test.mathematica)
                    )
                    )
            );
        }
        return list;
    }
}
