package gov.nist.drmf.interpreter.generic.macro;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import gov.nist.drmf.interpreter.common.tests.Resource;
import gov.nist.drmf.interpreter.common.tests.ResourceProvider;
import gov.nist.drmf.interpreter.pom.extensions.MatcherConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Andre Greiner-Petter
 */
public class MacroDefinitionTests {
    private static Map<String, MacroBean> loadedMacros;

    @BeforeAll
    public static void setup(String in) throws IOException {
        ResourceProvider.load(MacroDefinitionTests.class, "ExampleFuncDef.sty");
        MacroDefinitionStyleFileParser parser = new MacroDefinitionStyleFileParser();
        parser.load(in);
        loadedMacros = parser.getExtractedMacros();
    }

    @Resource("ExampleFunDefSerialized.json")
    public void serializerFullTest(String gold) throws IOException {
        // fully write serialized file to temp
        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        String serializedStream = mapper.writeValueAsString(loadedMacros);
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
        assertEquals(2, jacobiBean.getTex().size());

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

        List<MacroGenericSemanticEntry> tex = jacobiBean.getTex();
        assertEquals(2, tex.size(), tex.toString());
        assertEquals("P^{(par1,par2)}_{par3}", tex.get(1).getGenericTex());
        assertEquals("\\JacobipolyP{par1}{par2}{par3}@{x}", tex.get(1).getSemanticTex());

        assertEquals("P^{(par1,par2)}_{par3} (var1)", tex.get(0).getGenericTex());
        assertEquals("\\JacobipolyP{par1}{par2}{par3}@{var1}", tex.get(0).getSemanticTex());
    }

    @Test
    public void macroOptionalFerrerTest() {
        MacroBean ferrerBean = loadedMacros.get("FerrersP");
        assertNotNull(ferrerBean);
        assertEquals("FerrersP", ferrerBean.getName());

        List<MacroGenericSemanticEntry> tex = ferrerBean.getTex();
        assertEquals(4, tex.size(), tex.toString());

        assertEquals("\\mathsf{P}^{opPar1}_{par1} (var1)", tex.get(0).getGenericTex());
        assertEquals("\\FerrersP[opPar1]{par1}@{var1}", tex.get(0).getSemanticTex());

        assertEquals("\\mathsf{P}^{opPar1}_{par1}", tex.get(1).getGenericTex());
        assertEquals("\\FerrersP[opPar1]{par1}@{x}", tex.get(1).getSemanticTex());

        assertEquals("\\mathsf{P}_{par1} (var1)", tex.get(2).getGenericTex());
        assertEquals("\\FerrersP{par1}@{var1}", tex.get(2).getSemanticTex());

        assertEquals("\\mathsf{P}_{par1}", tex.get(3).getGenericTex());
        assertEquals("\\FerrersP{par1}@{x}", tex.get(3).getSemanticTex());
    }

    @Test
    public void multinomialTest() {
        MacroBean multiNBean = loadedMacros.get("multinomial");
        assertNotNull(multiNBean);
        assertEquals("multinomial", multiNBean.getName());
        assertEquals("the multinomial coefficient", multiNBean.getMetaInformation().getDescription());

        List<MacroGenericSemanticEntry> tex = multiNBean.getTex();
        assertEquals(1, tex.size(), tex.toString());
        assertEquals("\\left({par1 \\atop par2}\\right)", tex.get(0).getGenericTex());
        assertEquals("\\multinomial{par1}{par2}", tex.get(0).getSemanticTex());
    }

    @Test
    public void pochhammerTest() {
        MacroBean bean = loadedMacros.get("Pochhammersym");
        assertNotNull(bean);
        assertEquals("Pochhammersym", bean.getName());
        assertEquals("the Pochhammer symbol (or shifted factorial)", bean.getMetaInformation().getDescription());

        List<MacroGenericSemanticEntry> tex = bean.getTex();
        assertEquals(1, tex.size(), tex.toString());
        assertEquals("(par1)_{par2}", tex.get(0).getGenericTex());
        assertEquals("\\Pochhammersym{par1}{par2}", tex.get(0).getSemanticTex());
    }

    @Test
    public void riemannthetacharTest() {
        MacroBean bean = loadedMacros.get("Riemannthetachar");
        assertNotNull(bean);
        assertEquals("Riemannthetachar", bean.getName());
        assertEquals("the Riemann theta function with characteristics", bean.getMetaInformation().getDescription());

        List<MacroGenericSemanticEntry> tex = bean.getTex();
        assertEquals(2, tex.size(), tex.toString());
        assertEquals("\\theta\\left[{par1 \\atop par2}\\right] (var1 | var2)", tex.get(0).getGenericTex());
        assertEquals("\\Riemannthetachar{par1}{par2}@{var1}{var2}", tex.get(0).getSemanticTex());

        assertEquals("\\theta\\left[{par1 \\atop par2}\\right]", tex.get(1).getGenericTex());
        assertEquals("\\Riemannthetachar{par1}{par2}@{z}{\\Omega}", tex.get(1).getSemanticTex());
    }

    @Test
    public void invisibleCommaTest() {
        MacroBean bean = loadedMacros.get("LeviCivitasym");
        assertNotNull(bean);
        assertEquals("LeviCivitasym", bean.getName());

        List<MacroGenericSemanticEntry> tex = bean.getTex();
        assertEquals(1, tex.size(), tex.toString());
        assertEquals("\\epsilon_{par1 par2 par3}", tex.get(0).getGenericTex());
        assertEquals("\\LeviCivitasym{par1}{par2}{par3}", tex.get(0).getSemanticTex());
    }

    @Test
    public void optionalArgumentDirichletcharTest() {
        MacroBean bean = loadedMacros.get("Dirichletchar");
        assertNotNull(bean);
        assertEquals("Dirichletchar", bean.getName());

        List<MacroGenericSemanticEntry> tex = bean.getTex();
        assertEquals(6, tex.size(), tex.toString());

        assertEquals("\\chi_{opPar1} (var1,var2)", tex.get(0).getGenericTex(), tex.toString());
        assertEquals("\\chi_{opPar1} (var1)", tex.get(1).getGenericTex(), tex.toString());
        assertEquals("\\chi_{opPar1}", tex.get(2).getGenericTex(), tex.toString());
        assertEquals("\\chi (var1,var2)", tex.get(3).getGenericTex(), tex.toString());
        assertEquals("\\chi (var1)", tex.get(4).getGenericTex(), tex.toString());
        assertEquals("\\chi", tex.get(5).getGenericTex(), tex.toString());

        assertEquals("\\Dirichletchar[opPar1]@{var1}{var2}", tex.get(0).getSemanticTex());
        assertEquals("\\Dirichletchar[opPar1]@@{var1}{k}", tex.get(1).getSemanticTex());
        assertEquals("\\Dirichletchar[opPar1]@{n}{k}", tex.get(2).getSemanticTex());
        assertEquals("\\Dirichletchar@{var1}{var2}", tex.get(3).getSemanticTex());
        assertEquals("\\Dirichletchar@@{var1}{k}", tex.get(4).getSemanticTex());
        assertEquals("\\Dirichletchar@{n}{k}", tex.get(5).getSemanticTex());
    }

    @Test
    public void macroPosIntegersTest() {
        MacroBean posIntBean = loadedMacros.get("posIntegers");
        assertNotNull(posIntBean);
        assertEquals(0, posIntBean.getNumberOfOptionalParameters());
        assertEquals(0, posIntBean.getNumberOfParameters());
        assertEquals(0, posIntBean.getNumberOfArguments());

        assertEquals(1, posIntBean.getTex().size());
        assertEquals("\\posIntegers", posIntBean.getTex().get(0).getSemanticTex());
        assertEquals("\\mathbb{Z}^{+}", posIntBean.getTex().get(0).getGenericTex());
    }

    @Test
    public void macroDivisorTest() {
        MacroBean divBean = loadedMacros.get("ndivisors");
        assertNotNull(divBean);
        assertEquals(4, divBean.getTex().size());
        assertEquals("d_{opPar1} (var1)", divBean.getTex().get(0).getGenericTex(), divBean.getTex().toString());
        assertEquals("d_{opPar1}", divBean.getTex().get(1).getGenericTex(), divBean.getTex().toString());
        assertEquals("d (var1)", divBean.getTex().get(2).getGenericTex(), divBean.getTex().toString());
        assertEquals("d", divBean.getTex().get(3).getGenericTex(), divBean.getTex().toString());
    }

    @Test
    public void macroContinuousTest() {
        MacroBean b = loadedMacros.get("continuous");
        assertNotNull(b);
        assertEquals(4, b.getTex().size(), b.getTex().toString());
        assertEquals("(a,b)", b.getMetaInformation().getStandardArguments().getStandardVariables().getFirst());
        assertEquals("the set of continuous functions $n$-times differentiable on the interval $(a,b)$", b.getMetaInformation().getDescription());
    }

    @Test
    public void macroGenHyperFTest() {
        MacroBean b = loadedMacros.get("genhyperF");
        assertNotNull(b);
        assertNotNull(loadedMacros.get("genhyperF{1}{1}"));

        List<MacroGenericSemanticEntry> tex = b.getTex();
        assertEquals(4, tex.size(), tex.toString());

        MatcherConfig strictConfig = MatcherConfig.getExactMatchConfig();
        assertTrue( strictConfig.getIllegalTokensForWildcard("var1").contains(",") );
        assertTrue( strictConfig.getIllegalTokensForWildcard("var2").contains(",") );

        MacroHelper.updateMatchingConfig(b, strictConfig);
        assertFalse( strictConfig.getIllegalTokensForWildcard("var1").contains(",") );
        assertFalse( strictConfig.getIllegalTokensForWildcard("var2").contains(",") );
    }

    @Resource("JacobipolyPSerialized.json")
    public void serializerTest(String gold) throws IOException {
        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        MacroBean jacBean = loadedMacros.get("JacobipolyP");
        String jacBeanStr = mapper.writeValueAsString(jacBean);
        assertEquals(gold, jacBeanStr);
    }

    @Resource("JacobipolyPSerialized.json")
    public void deserializerTest(String jacobiSerialized) throws IOException {
        MacroBean jacGoldBean = loadedMacros.get("JacobipolyP");

        ObjectMapper mapper = new ObjectMapper();
        MacroBean jacDeserializeBean = mapper.readValue(jacobiSerialized, MacroBean.class);

        assertEquals( jacGoldBean.getName(), jacDeserializeBean.getName() );
        assertEquals( jacGoldBean.getTex(), jacDeserializeBean.getTex() );
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
