package gov.nist.drmf.interpreter.pom;

import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.pom.common.MathTermUtility;
import gov.nist.drmf.interpreter.pom.common.grammar.MathTermTags;
import gov.nist.drmf.interpreter.pom.extensions.PrintablePomTaggedExpression;
import mlp.FeatureSet;
import mlp.MathTerm;
import mlp.ParseException;
import mlp.PomTaggedExpression;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.*;

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
                "The semantic parser shall interpret cos as dlmf-macro not as a function"
        );

        second.setTag("fake");
        TreeSet<String> set = new TreeSet<>();
        set.add(MathTermTags.function.tag());
        Map<String, SortedSet<String>> map = new HashMap<>();
        map.put(Keys.FEATURE_ROLE, set);
        second.getAlternativeFeatureSets().add(new FeatureSet(map));
        assertTrue( MathTermUtility.isFunction(second),
                "If we delete the main tag, we fallback to the featureset role, which should be a function"
        );
    }

    @Test
    public void greekLetterMeaningTest() throws ParseException {
        String test = "\\alpha";
        PrintablePomTaggedExpression pte = mlp.parse(test);
        assertTrue( MathTermUtility.hasGreekLetterMeaning(pte.getRoot()) );

        pte = mlp.parse("x");
        assertFalse( MathTermUtility.hasGreekLetterMeaning(pte.getRoot()) );
    }

    @Test
    public void equalsCheckTest() throws ParseException {
        String test = "\\alpha";
        PrintablePomTaggedExpression pte = mlp.parse(test);
        assertTrue( MathTermUtility.equals(pte.getRoot(), MathTermTags.command) );

        pte = mlp.parse("a");
        assertTrue( MathTermUtility.equals(pte.getRoot(), MathTermTags.letter) );
    }
}
