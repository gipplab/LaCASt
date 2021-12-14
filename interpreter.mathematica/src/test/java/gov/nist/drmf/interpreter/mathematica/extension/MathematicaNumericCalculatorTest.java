package gov.nist.drmf.interpreter.mathematica.extension;

import gov.nist.drmf.interpreter.common.eval.*;
import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import gov.nist.drmf.interpreter.common.interfaces.IConstraintTranslator;
import gov.nist.drmf.interpreter.common.meta.DLMF;
import gov.nist.drmf.interpreter.mathematica.MathematicaConnector;
import gov.nist.drmf.interpreter.mathematica.common.AssumeMathematicaAvailability;
import gov.nist.drmf.interpreter.mathematica.wrapper.jlink.Expr;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author Andre Greiner-Petter
 */
@AssumeMathematicaAvailability
public class MathematicaNumericCalculatorTest {

    private static MathematicaNumericalCalculator calculator;
    private static Path numericConfigPath;

    @BeforeAll
    static void setup() throws ComputerAlgebraSystemEngineException {
        MathematicaConnector connector = new MathematicaConnector();
        connector.loadNumericProcedures();
        calculator = (MathematicaNumericalCalculator) connector.getNumericEvaluator();
        numericConfigPath = Paths.get(MathematicaNumericCalculatorTest.class.getResource("numerical_tests.properties").getPath());
    }

    @Test
    void simpleNumericCalcTest() throws ComputerAlgebraSystemEngineException {
        calculator.storeVariables(
                genSet("x"),
                genList("1", "2")
        );

        calculator.storeConstraintVariables(null, null);
        calculator.storeExtraVariables(genList("n"), genList("1"));

        String constN = calculator.setConstraints(null);
        String testValuesN = calculator.buildTestCases(constN, 10);

        Expr result = calculator.performGeneratedTestOnExpression(
                "x - 1", testValuesN, "", 10
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
        String lhs = "Binomial[n,k]";
        String rhs = "Divide[(n)!,(n - k)!*(k)!]";
        String test = "(Binomial[n,k]) - (Divide[(n)!,(n - k)!*(k)!])";

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
        testBench.setVariables(genSet("n, k"));
        NumericResult result = calculator.performNumericTest(testBench);
        // n and k where defined as special variables, so they were tested for 1,2,3,4 each which makes
        // 4*4 = 16 test combinations
        assertEquals(16, calculator.getPerformedTestCases());
        assertEquals(0, calculator.getNumberOfFailedTestCases());

        assertEquals(TestResultType.SUCCESS, result.overallResult());
        assertFalse(result.crashed());
        assertFalse(result.wasAborted());
        assertEquals(1, result.getTestCalculationsGroups().size());

        NumericCalculationGroup group = result.getTestCalculationsGroups().get(0);
        // n and k where defined as special variables, so they were tested for 1,2,3,4 each which makes
        // 4*4 = 16 test combinations
        assertEquals(16, group.getTestCalculations().size());
        for ( NumericCalculation nc : group.getTestCalculations() ) {
            assertEquals( TestResultType.SUCCESS, nc.getResult() );
            assertTrue( nc.getResultExpression().matches("0+\\.?0*") );
        }
    }

    private List<String> genList(String... elements) {
        return List.of(elements);
    }

    private Set<String> genSet(String... elements) {
        return Set.of(elements);
    }
}
