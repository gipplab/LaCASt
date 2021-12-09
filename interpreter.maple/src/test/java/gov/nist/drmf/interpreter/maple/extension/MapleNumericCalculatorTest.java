package gov.nist.drmf.interpreter.maple.extension;

import com.maplesoft.externalcall.MapleException;
import com.maplesoft.openmaple.Algebraic;
import gov.nist.drmf.interpreter.common.eval.*;
import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import gov.nist.drmf.interpreter.common.interfaces.IConstraintTranslator;
import gov.nist.drmf.interpreter.common.meta.DLMF;
import gov.nist.drmf.interpreter.maple.common.MapleScriptHandler;
import gov.nist.drmf.interpreter.maple.setup.AssumeMapleAvailability;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

/**
 * @author Andre Greiner-Petter
 */
@AssumeMapleAvailability
public class MapleNumericCalculatorTest {

    private static MapleNumericCalculator calculator;
    private static MapleScriptHandler scriptHandler;
    private static Path numericConfigPath;

    @BeforeAll
    static void setup() throws IOException, MapleException {
        calculator = new MapleNumericCalculator();
        numericConfigPath = Paths.get(MapleNumericCalculatorTest.class.getResource("numerical_tests.properties").getPath());
        scriptHandler = new MapleScriptHandler();

        for ( String script : scriptHandler.getNumericProcedures() ) {
            MapleInterface.getUniqueMapleInterface().evaluate(script);
        }
    }

    @Test
    void simpleNumericCalcTest() throws ComputerAlgebraSystemEngineException {
        // Following the steps required in AbstractCasEngineNumericalEvaluator#performNumericalTest(...)
        calculator.storeVariables(
                genSet("x"),
                genList("1", "2")
        );

        calculator.storeConstraintVariables(null, null);
        calculator.storeExtraVariables(genList("n"), genList("1"));

        String constN = calculator.setConstraints(null);
        String testValuesN = calculator.buildTestCases(constN, 10);

        Algebraic result = calculator.performGeneratedTestOnExpression(
                "x - 1", testValuesN, scriptHandler.getPostProcessingScriptName(true), 10
        );

        assertFalse(calculator.wasAborted(result));

        NumericCalculationGroup wrongCalcs = calculator.getNumericCalculationGroup(result);
        assertNotNull(wrongCalcs);
        assertEquals(2, wrongCalcs.getSize());
        assertEquals("0.", wrongCalcs.get(0).getResultExpression());
        assertEquals("1.", wrongCalcs.get(1).getResultExpression());

        Map<String, String> values = wrongCalcs.get(0).getTestValues();
        assertEquals(1, values.keySet().size());
        assertEquals("1", values.get("x"));
    }

    @Test
    @DLMF("1.2.E1")
    void numericEquivalenceTest() throws ComputerAlgebraSystemEngineException {
        String lhs = "binomial(n,k)";
        String rhs = "(factorial(n))/(factorial(n - k)*factorial(k))";
        String test = "(binomial(n,k)) - ((factorial(n))/(factorial(n - k)*factorial(k)))";

        // Mocking config to avoid actual translation calls, this module does not depend on the forward translator
        // and should not to just to test out numerical test behaviour
        NumericalConfig config = new NumericalConfig(numericConfigPath);
        NumericalConfig mockConfig = spy(config);
        doReturn(genList("-I", "I"))
                .when(mockConfig)
                .getListOfNumericalValues(Mockito.isNull(), Mockito.isNull());
        doReturn(genList("n", "k"))
                .when(mockConfig)
                .getListOfSpecialVariables(Mockito.isNull());
        doReturn(genList("1", "2", "3", "4"))
                .when(mockConfig)
                .getListOfSpecialVariableValues(Mockito.isNull());

        NumericalTest testBench = new NumericalTest(
                lhs, rhs, test,
                new INumericTestCase() {
                    @Override
                    public List<String> getConstraints(IConstraintTranslator translator, String label) {
                        return new LinkedList<>();
                    }

                    @Override
                    public List<String> getConstraintVariables(IConstraintTranslator translator, String label) {
                        return new LinkedList<>();
                    }

                    @Override
                    public List<String> getConstraintValues() {
                        return new LinkedList<>();
                    }


                },
                mockConfig,
                // no translator necessary
                null
        );
        testBench.setPostProcessingMethodName(scriptHandler.getPostProcessingScriptName(true));
        testBench.setVariables(genSet("n, k"));
        NumericResult result = calculator.performNumericTest(testBench);
        // n and k where defined as special variables, so they were tested for 1,2,3,4 each which makes
        // 4*4 = 16 test combinations
        assertEquals(16, calculator.getPerformedTestCases());
        assertEquals(0, calculator.getNumberOfFailedTestCases());

        assertFalse(result.crashed());
        assertFalse(result.wasAborted());

        // Maple may complain about factorial(-1) and throw a "division by zero" exception
        // So not all cases are necessarily successful
//        assertEquals(TestResultType.SUCCESS, result.overallResult(),
//                "The overall numeric evaluation of DLMF 1.2.1 was unsuccessful. Result list: " + buildStringOfNumericResult(result));
        assertEquals(1, result.getTestCalculationsGroups().size());

        NumericCalculationGroup group = result.getTestCalculationsGroups().get(0);
        // n and k where defined as special variables, so they were tested for 1,2,3,4 each which makes
        // 4*4 = 16 test combinations
        assertEquals(16, group.getTestCalculations().size());
        for ( NumericCalculation nc : group.getTestCalculations() ) {
            if ( isNGreaterK(nc.getTestValues()) ) {
                assertEquals( TestResultType.SUCCESS, nc.getResult(),
                        "Expected successful test: " + nc.toString() );
                assertTrue( nc.getResultExpression().matches("0+\\.?0*") );
            }
        }
    }

    private boolean isNGreaterK(Map<String, String> values) {
        int k = Integer.parseInt(values.get("k"));
        int n = Integer.parseInt(values.get("n"));
        return n >= k;
    }

    private List<String> genList(String... elements) {
        return List.of(elements);
    }

    private Set<String> genSet(String... elements) {
        return Set.of(elements);
    }
}
