package gov.nist.drmf.interpreter.generic.macro;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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
    public void loadedAllMacrosTest() {
        assertEquals(6, loadedMacros.keySet().size());
    }

    @Test
    public void macroJacobiTest() {
        MacroBean jacobiBean = loadedMacros.get("JacobipolyP");
        assertNotNull(jacobiBean);
        assertEquals("JacobipolyP", jacobiBean.getName());
        assertEquals(1, jacobiBean.getGenericLatex().size());
        assertEquals("P^{(par1,par2)}_{par3} (var1)", jacobiBean.getGenericLatex().getFirst());
        assertEquals("the Jacobi polynomial", jacobiBean.getDescription());
        assertEquals("Jacobi-polynomial-P", jacobiBean.getMeaning());
        assertEquals("orthpoly2_dlmf:Jacobi_P", jacobiBean.getOpenMathID());
        assertEquals( 3, jacobiBean.getNumberOfParameters());
        assertEquals( 1, jacobiBean.getNumberOfArguments());

        LinkedList<String> args = jacobiBean.getStandardArguments();
        LinkedList<String> params = jacobiBean.getStandardParameters();
        assertTrue(args.contains("x"));
        assertTrue(params.contains("\\alpha"));
        assertTrue(params.contains("\\beta"));
        assertTrue(params.contains("n"));

        assertEquals("\\JacobipolyP{$0}{$1}{$2}@{$3}", jacobiBean.getSemanticLaTeX());
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

        assertEquals("\\FerrersP[$0]{$1}@{$2}", ferrerBean.getSemanticLaTeX());
    }

    @Test
    public void macroPosIntegersTest() {
        MacroBean posIntBean = loadedMacros.get("posIntegers");
        assertNotNull(posIntBean);
        assertEquals(0, posIntBean.getNumberOfOptionalParameters());
        assertEquals(0, posIntBean.getNumberOfParameters());
        assertEquals(0, posIntBean.getNumberOfArguments());
        assertEquals("\\posIntegers", posIntBean.getSemanticLaTeX());
        assertEquals(1, posIntBean.getGenericLatex().size());
        assertEquals("{\\mathbb{Z}^{+}}", posIntBean.getGenericLatex().getFirst());
    }

    @Test
    public void macroDivisorTest() {
        MacroBean divBean = loadedMacros.get("ndivisors");
        assertNotNull(divBean);
        assertEquals(2, divBean.getGenericLatex().size());
        assertEquals("d_{opPar1} (var1)", divBean.getGenericLatex().get(0));
        assertEquals("d (var1)", divBean.getGenericLatex().get(1));
    }

    @Test
    public void macroContinuousTest() {
        MacroBean b = loadedMacros.get("continuous");
        assertNotNull(b);
        assertEquals(2, b.getGenericLatex().size());
        assertEquals("(a,b)", b.getStandardArguments().getFirst());
        assertEquals("the set of continuous functions $n$-times differentiable on the interval $(a,b)$", b.getDescription());
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
        assertEquals( jacGoldBean.getDescription(), jacDeserializeBean.getDescription() );
        assertEquals( jacGoldBean.getMeaning(), jacDeserializeBean.getMeaning() );
        assertEquals( jacGoldBean.getOpenMathID(), jacDeserializeBean.getOpenMathID() );
        assertEquals( jacGoldBean.getStandardParameters(), jacDeserializeBean.getStandardParameters() );
        assertEquals( jacGoldBean.getStandardArguments(), jacDeserializeBean.getStandardArguments() );
        assertEquals( jacGoldBean.getNumberOfArguments(), jacDeserializeBean.getNumberOfArguments() );
        assertEquals( jacGoldBean.getNumberOfOptionalParameters(), jacDeserializeBean.getNumberOfOptionalParameters() );
        assertEquals( jacGoldBean.getNumberOfParameters(), jacDeserializeBean.getNumberOfParameters() );
    }
}
