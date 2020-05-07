package gov.nist.drmf.interpreter.mlp;

import gov.nist.drmf.interpreter.mlp.extensions.PrintablePomTaggedExpression;
import mlp.MathTerm;
import mlp.ParseException;
import mlp.PomTaggedExpression;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Andre Greiner-Petter
 */
public class MathTermUtilityTests {
    private static SemanticMLPWrapper mlp;

    @BeforeAll
    public static void setup() throws IOException {
        mlp = SemanticMLPWrapper.getStandardInstance();
    }

    @Test
    public void isGreekLetterTest() throws ParseException {
        PrintablePomTaggedExpression pte = mlp.parse("pi \\pi");
        List<PomTaggedExpression> components = pte.getComponents();
        MathTerm first = components.get(0).getRoot();
        MathTerm second = components.get(1).getRoot();
        assertTrue( MathTermUtility.isGreekLetter(first) );
        assertTrue( MathTermUtility.isGreekLetter(second) );
    }

    @Test
    public void isFunctionTest() throws ParseException {
        String test = "A \\cos";
        PrintablePomTaggedExpression pte = mlp.parse(test);
        List<PomTaggedExpression> components = pte.getComponents();
        MathTerm first = components.get(0).getRoot();
        MathTerm second = components.get(1).getRoot();
        assertFalse( MathTermUtility.isFunction(first) );
        assertFalse( MathTermUtility.isFunction(second),
                "The semantic parser shall interpret cos as dlmf-macro not as a function" );
    }
}
