package gov.nist.drmf.interpreter.mathematica.extension;

import com.wolfram.jlink.Expr;
import gov.nist.drmf.interpreter.common.eval.TestResultType;
import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import gov.nist.drmf.interpreter.common.pojo.NumericCalculation;
import gov.nist.drmf.interpreter.mathematica.MathematicaConnector;
import gov.nist.drmf.interpreter.mathematica.common.AssumeMathematicaAvailability;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Andre Greiner-Petter
 */
@AssumeMathematicaAvailability
public class MathematicaNumericCalculatorTest {

    private static MathematicaNumericalCalculator calculator;

    @BeforeAll
    static void setup() throws ComputerAlgebraSystemEngineException {
        MathematicaConnector connector = new MathematicaConnector();
        connector.loadNumericProcedures();
        calculator = (MathematicaNumericalCalculator) connector.getNumericEvaluator();
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

        assertFalse( calculator.wasAborted(result) );
        assertEquals( TestResultType.FAILURE, calculator.getStatusOfResult(result) );

        List<NumericCalculation> wrongCalcs = calculator.getNumericCalculationList(result);
        assertNotNull(wrongCalcs);
        assertEquals( 1, wrongCalcs.size() );
        assertEquals( "1.0", wrongCalcs.get(0).getResult() );

        Map<String, String> values = wrongCalcs.get(0).getTestValues();
        assertEquals( 1, values.keySet().size() );
        assertEquals( "2", values.get("x") );
    }

    private List<String> genList(String... elements) {
        return List.of(elements);
    }

    private Set<String> genSet(String... elements) {
        return Set.of(elements);
    }
}
