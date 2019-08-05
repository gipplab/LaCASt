package gov.nist.drmf.interpreter.cas.translation.components;

import gov.nist.drmf.interpreter.cas.logging.TranslatedExpression;
import gov.nist.drmf.interpreter.cas.translation.SemanticLatexTranslator;
import gov.nist.drmf.interpreter.common.*;
import gov.nist.drmf.interpreter.mlp.extensions.MacrosLexicon;
import mlp.PomParser;
import mlp.PomTaggedExpression;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class MacroTranslatorMathematicaTest {

    private static SemanticLatexTranslator slt;
    private static MacroTranslator translator;

    @BeforeAll
    static void setUp() {
        GlobalConstants.CAS_KEY = Keys.KEY_MATHEMATICA;
        slt = new SemanticLatexTranslator(Keys.KEY_LATEX, Keys.KEY_MATHEMATICA);
        try {
            slt.init(GlobalPaths.PATH_REFERENCE_DATA);
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }

    /*
    @AfterEach
    void tearDown() {
    }
    */

    private static final String[] expression = {
            "\\AiryAi@{x}",
            "\\AiryAi'@{x}",
            "\\AiryAi@'{x}",
            "\\AiryAi^{(5)}@{x}",
            "\\EulerGamma@{0}",
            "\\EulerGamma'@{z}",
            "\\ln@@{\\EulerGamma@{a}}",
            "\\ln@@{2\\pi}",
            "\\EulerGamma^{(2+3n)}@{z}",
            "\\EulerGamma''''''''''@{z}",
            "\\BesselJ{\\nu}@{x}",
            "\\BesselJ{\\nu}'@{x}",
            "\\BesselJ{\\nu}^{(5^3)}@{x}",
            "\\BesselJ{\\nu}^{(5^3}@{x}",
            "\\BesselY{\\nu}@{z}",
            "\\BesselY{\\nu}'@{z}",
            "\\BesselY{\\nu}@'{z}",
            "\\BesselY{\\nu}^{(2n)}@{z}",
            "\\BesselY{\\nu}'@{\\cos@@{2z}}",
            "\\BesselY{\\ln@@{c}+2}'@{\\cos@@{2z}}",
            "\\BesselY^{(3)}'@@@{a}{b}{c}{z}",
            "\\BesselY'^{(3)}@@@{a}{b}{c}{z}",
            "\\compellintKk@{k}",
            "\\compellintKk'@{k}",
            "\\compellintKk^{(6)}@{k}",
            "\\AiryBi@{x}",
            "\\AiryBi'@{x}",
            "\\AiryBi^{(3)}@{x}",
            "\\pochhammer{a}{n}",
            "\\pochhammer'{a}{n}",
    };

    private static final String[] translatedMathematica = {
            "AiryAi[x]",
            "D[AiryAi[temp], {temp, 1}]/.temp-> x",
            null,
            "D[AiryAi[temp], {temp, 5}]/.temp-> x",
            "Gamma[0]",
            "D[Gamma[temp], {temp, 1}]/.temp-> z",
            "Log[Gamma[a]]",
            "Log[2 \\[Pi]]",
            "D[Gamma[temp], {temp, 2 + 3 n}]/.temp-> z",
            "D[Gamma[temp], {temp, 10}]/.temp-> z",
            "BesselJ[\\[Nu], x]",
            "D[BesselJ[\\[Nu], temp], {temp, 1}]/.temp-> x",
            "D[BesselJ[\\[Nu], temp], {temp, (5)^(3)}]/.temp-> x",
            null,
            "BesselY[\\[Nu], z]",
            "D[BesselY[\\[Nu], temp], {temp, 1}]/.temp-> z",
            null,
            "D[BesselY[\\[Nu], temp], {temp, 2 n}]/.temp-> z",
            "D[BesselY[\\[Nu], temp], {temp, 1}]/.temp-> Cos[2 z]",
            "D[BesselY[Log[c] + 2, temp], {temp, 1}]/.temp-> Cos[2 z]",
            null,
            null,
            "EllipticK[(k)^2]",
            "D[EllipticK[(temp)^2], {temp, 1}]/.temp-> k",
            "D[EllipticK[(temp)^2], {temp, 6}]/.temp-> k",
            "AiryBi[x]",
            "D[AiryBi[temp], {temp, 1}]/.temp-> x",
            "D[AiryBi[temp], {temp, 3}]/.temp-> x",
            "Pochhammer[a, n]",
            null,
    };

    private static final Class[] expectedExceptions = {
            null,
            null,
            TranslationException.class,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            TranslationException.class,
            null,
            null,
            TranslationException.class,
            null,
            null,
            null,
            TranslationException.class,
            TranslationException.class,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            TranslationException.class,
    };

    @TestFactory
    Stream<DynamicTest> differentiationMathematicaTest() {
        PomParser parser = new PomParser(GlobalPaths.PATH_REFERENCE_DATA.toString());
        parser.addLexicons( MacrosLexicon.getDLMFMacroLexicon() );

        translator = new MacroTranslator();

        List<String> expressions = Arrays.asList(expression);
        List<String> output = Arrays.asList(translatedMathematica);
        List<Class> exceptions = Arrays.asList(expectedExceptions);

        return expressions
                .stream()
                .map(
                        exp -> DynamicTest.dynamicTest("Expression: " + exp, () -> {
                            int index = expressions.indexOf(exp);
                            PomTaggedExpression ex = parser.parse(TeXPreProcessor.preProcessingTeX(expressions.get(index)));

                            translator.clearTranslation();

                            List<PomTaggedExpression> components = ex.getComponents();
                            PomTaggedExpression first = components.remove(0);

                            //System.out.println(translator.parseGeneral( first, components ));

                            if( exceptions.get(index) != null ) {
                                assertThrows( exceptions.get(index), () -> translator.translate(first, components) );
                            } else{
                                translator.translate(first, components);
                                assertEquals(output.get(index), translator.getTranslatedExpression());
                            }
                        }));

    }

}