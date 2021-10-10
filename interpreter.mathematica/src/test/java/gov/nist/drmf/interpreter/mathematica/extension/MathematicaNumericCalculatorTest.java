package gov.nist.drmf.interpreter.mathematica.extension;

import gov.nist.drmf.interpreter.common.eval.NumericCalculationGroup;
import gov.nist.drmf.interpreter.common.eval.TestResultType;
import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import gov.nist.drmf.interpreter.mathematica.MathematicaConnector;
import gov.nist.drmf.interpreter.mathematica.common.AssumeMathematicaAvailability;
import gov.nist.drmf.interpreter.mathematica.wrapper.Expr;
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

        NumericCalculationGroup wrongCalcs = calculator.getNumericCalculationGroup(result);
        assertNotNull(wrongCalcs);
        assertEquals( 2, wrongCalcs.getSize() );
        assertEquals( "0.", wrongCalcs.get(0).getResultExpression() );
        assertEquals( "1.", wrongCalcs.get(1).getResultExpression() );

        Map<String, String> values = wrongCalcs.get(0).getTestValues();
        assertEquals( 1, values.keySet().size() );
        assertEquals( "1", values.get("x") );
    }

    private List<String> genList(String... elements) {
        return List.of(elements);
    }

    private Set<String> genSet(String... elements) {
        return Set.of(elements);
    }
}
