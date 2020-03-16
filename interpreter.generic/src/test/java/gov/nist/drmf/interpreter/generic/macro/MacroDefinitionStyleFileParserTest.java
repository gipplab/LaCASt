package gov.nist.drmf.interpreter.generic.macro;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Andre Greiner-Petter
 */
public class MacroDefinitionStyleFileParserTest {
    private static final Logger LOG = LogManager.getLogger(MacroDefinitionStyleFileParserTest.class.getName());

    @Test
    public void macroLoaderTest() throws IOException {
        String in = readResource("ExampleFuncDef.sty");
        MacroDefinitionStyleFileParser parser = new MacroDefinitionStyleFileParser();
        parser.load(in);

        List<MacroBean> macros = parser.getListOfExtractedMacros();
        assertEquals(3, macros.size());

        MacroBean jacobiBean = macros.get(1);
        assertEquals("JacobipolyP", jacobiBean.getName());
        assertEquals(1, jacobiBean.getGenericLatex().size());
        assertEquals("P^{(par1,par2)}_{par3}(var1)", jacobiBean.getGenericLatex().getFirst());
        assertEquals("the Jacobi polynomial", jacobiBean.getDescription());
        assertEquals("Jacobi-polynomial-P", jacobiBean.getMeaning());
        assertEquals("orthpoly2_dlmf:Jacobi_P", jacobiBean.getOpenMathID());

        LinkedList<String> args = jacobiBean.getStandardArguments();
        LinkedList<String> params = jacobiBean.getStandardParameters();
        assertTrue(args.contains("x"));
        assertTrue(params.contains("\\alpha"));
        assertTrue(params.contains("\\beta"));
        assertTrue(params.contains("n"));
    }

    private String readResource(String fileName) throws IOException {
        try (InputStream is = this.getClass().getResourceAsStream(fileName)) {
            return IOUtils.toString(is, StandardCharsets.UTF_8);
        } catch ( IOException ioe ) {
            LOG.error("Cannot load resource file " + fileName, ioe);
            throw ioe;
        }
    }
}
