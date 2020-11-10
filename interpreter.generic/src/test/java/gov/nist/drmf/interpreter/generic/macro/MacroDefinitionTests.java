package gov.nist.drmf.interpreter.generic.macro;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import gov.nist.drmf.interpreter.pom.extensions.MatcherConfig;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Andre Greiner-Petter
 */
public class MacroDefinitionTests {
    private static final Logger LOG = LogManager.getLogger(MacroDefinitionTests.class.getName());

    private static Map<String, MacroBean> loadedMacros;

    private static final String fileName = "ExampleFunDefSerialized.json";

    @BeforeAll
    public static void setup() throws IOException {
        String in = readResource("ExampleFuncDef.sty");
        MacroDefinitionStyleFileParser parser = new MacroDefinitionStyleFileParser();
        parser.load(in);
        loadedMacros = parser.getExtractedMacros();
    }

    private static String readResource(String fileName) throws IOException {
        try (InputStream is = MacroDefinitionTests.class
                .getResourceAsStream(fileName)) {
            return IOUtils.toString(is, StandardCharsets.UTF_8);
        } catch ( IOException ioe ) {
            LOG.error("Cannot load resource file " + fileName, ioe);
            throw ioe;
        }
    }

    @Test
    public void serializerFullTest() throws IOException {
        // fully write serialized file to temp
        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        String serializedStream = mapper.writeValueAsString(loadedMacros);
        String gold = readResource(fileName);
        assertEquals(gold, serializedStream);
    }

    @Test
    public void loadedAllMacrosTest() {
        assertEquals(13, loadedMacros.keySet().size());
    }

    @Test
    public void macroJacobiTest() {
        MacroBean jacobiBean = loadedMacros.get("JacobipolyP");
        assertNotNull(jacobiBean);
        assertEquals("JacobipolyP", jacobiBean.getName());
        assertEquals(1, jacobiBean.getGenericLatex().size());
        assertEquals("P^{(par1,par2)}_{par3} (var1)", jacobiBean.getGenericLatex().getFirst());
        assertEquals("the Jacobi polynomial", jacobiBean.getMetaInformation().getDescription());
        assertEquals("Jacobi-polynomial-P", jacobiBean.getMetaInformation().getMeaning());
        assertEquals("orthpoly2_dlmf:Jacobi_P", jacobiBean.getMetaInformation().getOpenMathID());
        assertEquals( 3, jacobiBean.getNumberOfParameters());
        assertEquals( 1, jacobiBean.getNumberOfArguments());

        LinkedList<String> args = jacobiBean.getMetaInformation().getStandardArguments().getStandardVariables();
        LinkedList<String> params = jacobiBean.getMetaInformation().getStandardArguments().getStandardParameters();
        assertTrue(args.contains("x"));
        assertTrue(params.contains("\\alpha"));
        assertTrue(params.contains("\\beta"));
        assertTrue(params.contains("n"));

        assertEquals(1, jacobiBean.getSemanticLaTeX().size(), jacobiBean.getSemanticLaTeX().toString());
        assertEquals("\\JacobipolyP{par1}{par2}{par3}@{var1}", jacobiBean.getSemanticLaTeX().get(0));
    }

    @Test
    public void macroOptionalFerrerTest() {
        MacroBean ferrerBean = loadedMacros.get("FerrersP");
        assertNotNull(ferrerBean);
        assertEquals("FerrersP", ferrerBean.getName());

        List<String> genericLaTeX = ferrerBean.getGenericLatex();
        assertEquals(2, genericLaTeX.size());
        assertEquals("\\mathsf{P}^{opPar1}_{par1} (var1)", genericLaTeX.get(0));
        assertEquals("\\mathsf{P}_{par1} (var1)", genericLaTeX.get(1));

        List<String> semanticLaTeX = ferrerBean.getSemanticLaTeX();
        assertEquals(2, semanticLaTeX.size(), semanticLaTeX.toString());
        assertEquals("\\FerrersP[opPar1]{par1}@{var1}", semanticLaTeX.get(0));
        assertEquals("\\FerrersP{par1}@{var1}", semanticLaTeX.get(1));
    }

    @Test
    public void multinomialTest() {
        MacroBean multiNBean = loadedMacros.get("multinomial");
        assertNotNull(multiNBean);
        assertEquals("multinomial", multiNBean.getName());
        assertEquals("the multinomial coefficient", multiNBean.getMetaInformation().getDescription());

        List<String> genericLaTeX = multiNBean.getGenericLatex();
        assertEquals(1, genericLaTeX.size());
        assertEquals("\\left({par1 \\atop par2}\\right)", genericLaTeX.get(0));

        List<String> semanticLaTeX = multiNBean.getSemanticLaTeX();
        assertEquals(1, semanticLaTeX.size(), semanticLaTeX.toString());
        assertEquals("\\multinomial{par1}{par2}", semanticLaTeX.get(0));
    }

    @Test
    public void pochhammerTest() {
        MacroBean bean = loadedMacros.get("Pochhammersym");
        assertNotNull(bean);
        assertEquals("Pochhammersym", bean.getName());
        assertEquals("the Pochhammer symbol (or shifted factorial)", bean.getMetaInformation().getDescription());

        List<String> genericLaTeX = bean.getGenericLatex();
        assertEquals(1, genericLaTeX.size());
        assertEquals("(par1)_{par2}", genericLaTeX.get(0));

        List<String> semanticLaTeX = bean.getSemanticLaTeX();
        assertEquals(1, semanticLaTeX.size(), semanticLaTeX.toString());
        assertEquals("\\Pochhammersym{par1}{par2}", semanticLaTeX.get(0));
    }

    @Test
    public void riemannthetacharTest() {
        MacroBean bean = loadedMacros.get("Riemannthetachar");
        assertNotNull(bean);
        assertEquals("Riemannthetachar", bean.getName());
        assertEquals("the Riemann theta function with characteristics", bean.getMetaInformation().getDescription());

        List<String> genericLaTeX = bean.getGenericLatex();
        assertEquals(1, genericLaTeX.size());
        assertEquals("\\theta\\left[{par1 \\atop par2}\\right] (var1 | var2)", genericLaTeX.get(0));

        List<String> semanticLaTeX = bean.getSemanticLaTeX();
        assertEquals(1, semanticLaTeX.size(), semanticLaTeX.toString());
        assertEquals("\\Riemannthetachar{par1}{par2}@{var1}{var2}", semanticLaTeX.get(0));
    }

    @Test
    public void invisibleCommaTest() {
        MacroBean bean = loadedMacros.get("LeviCivitasym");
        assertNotNull(bean);
        assertEquals("LeviCivitasym", bean.getName());

        List<String> genericLaTeX = bean.getGenericLatex();
        assertEquals(1, genericLaTeX.size());
        assertEquals("\\epsilon_{par1 par2 par3}", genericLaTeX.get(0));

        List<String> semanticLaTeX = bean.getSemanticLaTeX();
        assertEquals(1, semanticLaTeX.size());
        assertEquals("\\LeviCivitasym{par1}{par2}{par3}", semanticLaTeX.get(0));
    }

    @Test
    public void optionalArgumentDirichletcharTest() {
        MacroBean bean = loadedMacros.get("Dirichletchar");
        assertNotNull(bean);
        assertEquals("Dirichletchar", bean.getName());

        List<String> genericLaTeX = bean.getGenericLatex();
        assertEquals(4, genericLaTeX.size(), genericLaTeX.toString());
        assertEquals("\\chi_{opPar1} (var1,var2)", genericLaTeX.get(0), genericLaTeX.toString());
        assertEquals("\\chi_{opPar1} (var1)", genericLaTeX.get(1), genericLaTeX.toString());
        assertEquals("\\chi (var1,var2)", genericLaTeX.get(2), genericLaTeX.toString());
        assertEquals("\\chi (var1)", genericLaTeX.get(3), genericLaTeX.toString());

        List<String> semanticLaTeX = bean.getSemanticLaTeX();
        assertEquals(4, semanticLaTeX.size(), semanticLaTeX.toString());
        assertEquals("\\Dirichletchar[opPar1]@{var1}{var2}", semanticLaTeX.get(0));
        assertEquals("\\Dirichletchar[opPar1]@@{var1}{k}", semanticLaTeX.get(1));
        assertEquals("\\Dirichletchar@{var1}{var2}", semanticLaTeX.get(2));
        assertEquals("\\Dirichletchar@@{var1}{k}", semanticLaTeX.get(3));
    }

    @Test
    public void macroPosIntegersTest() {
        MacroBean posIntBean = loadedMacros.get("posIntegers");
        assertNotNull(posIntBean);
        assertEquals(0, posIntBean.getNumberOfOptionalParameters());
        assertEquals(0, posIntBean.getNumberOfParameters());
        assertEquals(0, posIntBean.getNumberOfArguments());
        assertEquals(1, posIntBean.getSemanticLaTeX().size());
        assertEquals("\\posIntegers", posIntBean.getSemanticLaTeX().get(0));
        assertEquals(1, posIntBean.getGenericLatex().size());
        assertEquals("\\mathbb{Z}^{+}", posIntBean.getGenericLatex().getFirst());
    }

    @Test
    public void macroDivisorTest() {
        MacroBean divBean = loadedMacros.get("ndivisors");
        assertNotNull(divBean);
        assertEquals(2, divBean.getGenericLatex().size());
        assertEquals("d_{opPar1} (var1)", divBean.getGenericLatex().get(0), divBean.getGenericLatex().toString());
        assertEquals("d (var1)", divBean.getGenericLatex().get(1), divBean.getGenericLatex().toString());
    }

    @Test
    public void macroContinuousTest() {
        MacroBean b = loadedMacros.get("continuous");
        assertNotNull(b);
        assertEquals(2, b.getGenericLatex().size());
        assertEquals("(a,b)", b.getMetaInformation().getStandardArguments().getStandardVariables().getFirst());
        assertEquals("the set of continuous functions $n$-times differentiable on the interval $(a,b)$", b.getMetaInformation().getDescription());
    }

    @Test
    public void macroGenHyperFTest() {
        MacroBean b = loadedMacros.get("genhyperF");
        assertNotNull(b);
        assertNotNull(loadedMacros.get("genhyperF{1}{1}"));

        assertEquals(3, b.getGenericLatex().size());
        MatcherConfig strictConfig = MatcherConfig.getExactMatchConfig();
        assertTrue( strictConfig.getIllegalTokensForWildcard("var1").contains(",") );
        assertTrue( strictConfig.getIllegalTokensForWildcard("var2").contains(",") );

        MacroHelper.updateMatchingConfig(b, strictConfig);
        assertFalse( strictConfig.getIllegalTokensForWildcard("var1").contains(",") );
        assertFalse( strictConfig.getIllegalTokensForWildcard("var2").contains(",") );
    }

    @Test
    public void serializerTest() throws IOException {
        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        MacroBean jacBean = loadedMacros.get("JacobipolyP");
        String jacBeanStr = mapper.writeValueAsString(jacBean);

        String gold = readResource("JacobipolyPSerialized.json");
        assertEquals(gold, jacBeanStr);
    }

    @Test
    public void deserializerTest() throws IOException {
        String jacobiSerialized = readResource("JacobipolyPSerialized.json");
        MacroBean jacGoldBean = loadedMacros.get("JacobipolyP");

        ObjectMapper mapper = new ObjectMapper();
        MacroBean jacDeserializeBean = mapper.readValue(jacobiSerialized, MacroBean.class);

        assertEquals( jacGoldBean.getName(), jacDeserializeBean.getName() );
        assertEquals( jacGoldBean.getGenericLatex(), jacDeserializeBean.getGenericLatex() );
        assertEquals( jacGoldBean.getSemanticLaTeX(), jacDeserializeBean.getSemanticLaTeX() );
        assertEquals( jacGoldBean.getMetaInformation().getDescription(), jacDeserializeBean.getMetaInformation().getDescription() );
        assertEquals( jacGoldBean.getMetaInformation().getMeaning(), jacDeserializeBean.getMetaInformation().getMeaning() );
        assertEquals( jacGoldBean.getMetaInformation().getOpenMathID(), jacDeserializeBean.getMetaInformation().getOpenMathID() );
        assertEquals( jacGoldBean.getMetaInformation().getStandardArguments().getStandardParameters(), jacDeserializeBean.getMetaInformation().getStandardArguments().getStandardParameters() );
        assertEquals( jacGoldBean.getMetaInformation().getStandardArguments().getStandardVariables(), jacDeserializeBean.getMetaInformation().getStandardArguments().getStandardVariables() );
        assertEquals( jacGoldBean.getNumberOfArguments(), jacDeserializeBean.getNumberOfArguments() );
        assertEquals( jacGoldBean.getNumberOfOptionalParameters(), jacDeserializeBean.getNumberOfOptionalParameters() );
        assertEquals( jacGoldBean.getNumberOfParameters(), jacDeserializeBean.getNumberOfParameters() );
    }
}
