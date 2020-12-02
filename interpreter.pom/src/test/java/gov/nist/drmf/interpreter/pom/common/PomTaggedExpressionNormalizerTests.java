package gov.nist.drmf.interpreter.pom.common;

import gov.nist.drmf.interpreter.pom.SemanticMLPWrapper;
import gov.nist.drmf.interpreter.pom.common.meta.AssumeMLPAvailability;
import gov.nist.drmf.interpreter.pom.extensions.PrintablePomTaggedExpression;
import mlp.ParseException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Andre Greiner-Petter
 */
@AssumeMLPAvailability
public class PomTaggedExpressionNormalizerTests {

    private static SemanticMLPWrapper mlp;

    @BeforeAll
    static void setup() {
        mlp = SemanticMLPWrapper.getStandardInstance();
    }

    @Test
    void simplyScriptNormalizingTest() throws ParseException {
        PrintablePomTaggedExpression ppte = mlp.parse("a^n_m");
        checkList( ppte.getPrintableComponents(),
                "a", "^n_m"
        );
        checkList( ppte.getPrintableComponents().get(1).getPrintableComponents(),
                "^n", "_m"
        );

        PomTaggedExpressionNormalizer.normalize(ppte);
        checkList( ppte.getPrintableComponents(),
                "a", "_m^n"
        );
        checkList( ppte.getPrintableComponents().get(1).getPrintableComponents(),
                "_m", "^n"
        );
    }

    @Test
    void simplyOnlyScriptNormalizingTest() throws ParseException {
        PrintablePomTaggedExpression ppte = mlp.parse("a^n_m");

        // only parenthesis normalizing shouldn't change anything
        PomTaggedExpressionNormalizer.normalize(ppte, PomTaggedExpressionNormalizer.NORMALIZE_PARENTHESES);
        checkList( ppte.getPrintableComponents(),
                "a", "^n_m"
        );
        checkList( ppte.getPrintableComponents().get(1).getPrintableComponents(),
                "^n", "_m"
        );

        PomTaggedExpressionNormalizer.normalize(ppte, PomTaggedExpressionNormalizer.NORMALIZE_SUB_SUPERSCRIPTS);
        checkList( ppte.getPrintableComponents(),
                "a", "_m^n"
        );
        checkList( ppte.getPrintableComponents().get(1).getPrintableComponents(),
                "_m", "^n"
        );
    }

    @Test
    void parenthesisTest() throws ParseException {
        PrintablePomTaggedExpression ppte = mlp.parse("(a)");
        checkList( ppte.getPrintableComponents(),
                "(", "a", ")"
        );

        PomTaggedExpressionNormalizer.normalize(ppte, PomTaggedExpressionNormalizer.NORMALIZE_PARENTHESES);
        checkList( ppte.getPrintableComponents(),
                "(", "a", ")"
        );
    }

    @Test
    void normalizeParenthesisTest() throws ParseException {
        PrintablePomTaggedExpression ppte = mlp.parse("\\left( a \\right)");
        checkList( ppte.getPrintableComponents(),
                "\\left(", "a", "\\right)"
        );

        PomTaggedExpressionNormalizer.normalize(ppte, PomTaggedExpressionNormalizer.NORMALIZE_PARENTHESES);
        checkList( ppte.getPrintableComponents(),
                "(", "a", ")"
        );
    }

    @Test
    void normalizeBracketsTest() throws ParseException {
        PrintablePomTaggedExpression ppte = mlp.parse("\\left[ a \\right]");
        checkList( ppte.getPrintableComponents(),
                "\\left[", "a", "\\right]"
        );

        PomTaggedExpressionNormalizer.normalize(ppte, PomTaggedExpressionNormalizer.NORMALIZE_PARENTHESES);
        checkList( ppte.getPrintableComponents(),
                "[", "a", "]"
        );
    }

    @Test
    void normalizeCurlyBracketsTest() throws ParseException {
        PrintablePomTaggedExpression ppte = mlp.parse("\\left\\{ a \\right\\}");
        checkList( ppte.getPrintableComponents(),
                "\\left\\{", "a", "\\right\\}"
        );

        PomTaggedExpressionNormalizer.normalize(ppte, PomTaggedExpressionNormalizer.NORMALIZE_PARENTHESES);
        checkList( ppte.getPrintableComponents(),
                "\\{", "a", "\\}"
        );
    }

    private void checkList(List<PrintablePomTaggedExpression> components, String... matches ) {
        assertEquals(matches.length, components.size(), "Length doesnt match: [" +
                components.stream().map(PrintablePomTaggedExpression::getTexString).collect(Collectors.joining(", ")) + "] VS " + Arrays.toString(matches));
        for ( int i = 0; i < matches.length; i++ ){
            assertEquals(matches[i], components.get(i).getTexString());
        }
    }
}
