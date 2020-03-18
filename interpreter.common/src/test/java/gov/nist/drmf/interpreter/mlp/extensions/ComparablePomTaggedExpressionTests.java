package gov.nist.drmf.interpreter.mlp.extensions;

import gov.nist.drmf.interpreter.common.meta.AssumeMLPAvailability;
import gov.nist.drmf.interpreter.mlp.MLPWrapper;
import mlp.ParseException;
import mlp.PomTaggedExpression;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Andre Greiner-Petter
 */
@AssumeMLPAvailability
public class ComparablePomTaggedExpressionTests {
    private static MLPWrapper mlp;

    private static PomTaggedExpression simpleBlueprint;

    @BeforeAll
    public static void setup() throws ParseException {
        mlp = new MLPWrapper();
        simpleBlueprint = mlp.parse("a+WILD+c");
    }

    @Test
    public void simpleConstructorTest() {
        ComparablePomTaggedExpression blueprint = new ComparablePomTaggedExpression(simpleBlueprint, "WILD");
        assertTrue(blueprint.getMatches().isEmpty());
    }

    @Test
    public void linearMatchTest() throws ParseException {
        ComparablePomTaggedExpression blueprint = new ComparablePomTaggedExpression(simpleBlueprint, "WILD");
        PomTaggedExpression pte = mlp.parse("a+b+c");
        assertTrue( blueprint.match(pte) );
        assertTrue( blueprint.getMatches().containsKey("WILD") );
        assertEquals( "$ b$", blueprint.getMatches().get("WILD").toDollarMarkedString() );
    }
}
