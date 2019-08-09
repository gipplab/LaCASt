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

class MacroTranslatorMapleTest {

    private static SemanticLatexTranslator slt;
    private static MacroTranslator translator;

    @BeforeAll
    static void setUp() {
        GlobalConstants.CAS_KEY = Keys.KEY_MAPLE;
        slt = new SemanticLatexTranslator(Keys.KEY_LATEX, Keys.KEY_MAPLE);
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
            "\\Hurwitzzeta@{s}{a}",
            "\\Hurwitzzeta'@{0}{a}",
            "\\ln@@{\\EulerGamma@{a}}",
            "\\ln@@{2\\pi}",
            "\\Hurwitzzeta^{(2+3n)}@{s}{a}",
            "\\Hurwitzzeta''''''''''@{s}{a}",
            "\\modBesselKimag{\\nu}@{x}",
            "\\modBesselKimag{\\nu}'@{x}",
            "\\modBesselKimag{\\nu}^{(5^3)}@{x}",
            "\\modBesselKimag{\\nu}^{(5^3}@{x}",
            "\\hyperF@@@{a}{b}{c}{z}",
            "\\hyperF'@@@{a}{b}{c}{z}",
            "\\hyperF@'@@{a}{b}{c}{z}",
            "\\hyperF^{(2n)}@@@{a}{b}{c}{z}",
            "\\hyperF'@@@{a}{b}{c}{\\cos@@{2z}}",
            "\\hyperF'@@@{a}{b}{\\ln@@{c}+2}{\\cos@@{2z}}",
            "\\hyperF^{(3)}'@@@{a}{b}{c}{z}",
            "\\hyperF'^{(3)}@@@{a}{b}{c}{z}",
            "\\FerrersP[\\mu]{\\nu}@{x}",
            "\\FerrersP[\\mu]{\\nu}'@{x}",
            "\\FerrersP'[\\mu]{\\nu}@{x}",
            "\\FerrersP[\\mu]'{\\nu}@{x}",
            "\\FerrersP[\\mu]{\\nu}^{(6)}@{x}",
            "\\notmacro@{x}",
            "\\notmacro'@{x}",
            "\\notmacro^{(3)}@{x}",
            "\\pochhammer{a}{n}",
            "\\pochhammer'{a}{n}",
            "\\AiryAi'@{\\AiryBi@{x}}",
            "\\AiryAi@{\\AiryBi''@{x}}",
            "\\AiryAi'''@{\\AiryBi''@{x}}",
            "\\AiryAi^{(n)}@{\\AiryBi'''@{x}}",
            "\\AiryAi'''@{\\AiryBi^{(n)}@{x}}",
            "\\AiryAi'''@{\\AiryBi^{(n)}@{\\EulerGamma@{z}}}",
            "\\AiryAi'''@{\\AiryBi^{(n)}@{\\EulerGamma'@{z}}}",
            "\\Wronskian@{\\AiryAi@{z}, \\AiryBi@{z}}",
            "\\Wronskian@{\\OlverconfhyperM@{a}{b}{z}, z^{1-b} \\OlverconfhyperM@{a-b+1}{2-b}{z}}",
            "\\Wronskian@{z^{1-b} \\OlverconfhyperM@{a-b+1}{2-b}{z}, \\KummerconfhyperU@{a}{b}{z}}"
    };

    private static final String[] translatedMaple = {
            "AiryAi(x)",
            "subs( temp=x, diff( AiryAi(temp), temp$(1) ) )",
            null,
            "subs( temp=x, diff( AiryAi(temp), temp$(5) ) )",
            "Zeta(0, s, a)",
            "subs( temp=0, diff( Zeta(0, temp, a), temp$(1) ) )",
            "ln(GAMMA(a))",
            "ln(2*pi)",
            "subs( temp=s, diff( Zeta(0, temp, a), temp$(2 + 3*n) ) )",
            "subs( temp=s, diff( Zeta(0, temp, a), temp$(10) ) )",
            "BesselK(I*(nu), x)",
            "subs( temp=x, diff( BesselK(I*(nu), temp), temp$(1) ) )",
            "subs( temp=x, diff( BesselK(I*(nu), temp), temp$((5)^(3)) ) )",
            null,
            "hypergeom([a, b], [c], z)",
            "subs( temp=z, diff( hypergeom([a, b], [c], temp), temp$(1) ) )",
            null,
            "subs( temp=z, diff( hypergeom([a, b], [c], temp), temp$(2*n) ) )",
            "subs( temp=cos(2*z), diff( hypergeom([a, b], [c], temp), temp$(1) ) )",
            "subs( temp=cos(2*z), diff( hypergeom([a, b], [ln(c)+ 2], temp), temp$(1) ) )",
            null,
            null,
            "LegendreP(nu, mu, x)",
            "subs( temp=x, diff( LegendreP(nu, mu, temp), temp$(1) ) )",
            null,
            null,
            "subs( temp=x, diff( LegendreP(nu, mu, temp), temp$(6) ) )",
            null,
            null,
            null,
            "pochhammer(a, n)",
            null,
            "subs( temp=AiryBi(x), diff( AiryAi(temp), temp$(1) ) )",
            "AiryAi(subs( temp=x, diff( AiryBi(temp), temp$(2) ) ))",
            "subs( temp=subs( temp=x, diff( AiryBi(temp), temp$(2) ) ), diff( AiryAi(temp), temp$(3) ) )",
            "subs( temp=subs( temp=x, diff( AiryBi(temp), temp$(3) ) ), diff( AiryAi(temp), temp$(n) ) )",
            "subs( temp=subs( temp=x, diff( AiryBi(temp), temp$(n) ) ), diff( AiryAi(temp), temp$(3) ) )",
            "subs( temp=subs( temp=GAMMA(z), diff( AiryBi(temp), temp$(n) ) ), diff( AiryAi(temp), temp$(3) ) )",
            "subs( temp=subs( temp=subs( temp=z, diff( GAMMA(temp), temp$(1) ) ), diff( AiryBi(temp), temp$(n) ) ), diff( AiryAi(temp), temp$(3) ) )",
            "(AiryAi(z))*diff(AiryBi(z), z)-diff(AiryAi(z), z)*(AiryBi(z))",
            "(KummerM(a, b, z)/GAMMA(b))*diff((z)^(1 - b)* KummerM(a - b + 1, 2 - b, z)/GAMMA(2 - b), z)-diff(KummerM(a, b, z)/GAMMA(b), z)*((z)^(1 - b)* KummerM(a - b + 1, 2 - b, z)/GAMMA(2 - b))",
            "((z)^(1 - b)* KummerM(a - b + 1, 2 - b, z)/GAMMA(2 - b))*diff(KummerU(a, b, z), z)-diff((z)^(1 - b)* KummerM(a - b + 1, 2 - b, z)/GAMMA(2 - b), z)*(KummerU(a, b, z))",
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
            TranslationException.class,
            TranslationException.class,
            null,
            TranslationException.class,
            TranslationException.class,
            TranslationException.class,
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
    };

    @TestFactory
    Stream<DynamicTest> differentiationMapleTest() {
        PomParser parser = new PomParser(GlobalPaths.PATH_REFERENCE_DATA.toString());
        parser.addLexicons( MacrosLexicon.getDLMFMacroLexicon() );

        translator = new MacroTranslator();

        List<String> expressions = Arrays.asList(expression);
        List<String> output = Arrays.asList(translatedMaple);
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