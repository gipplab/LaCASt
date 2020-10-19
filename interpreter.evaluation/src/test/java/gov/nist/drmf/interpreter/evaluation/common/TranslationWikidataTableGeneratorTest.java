package gov.nist.drmf.interpreter.evaluation.common;

import gov.nist.drmf.interpreter.common.exceptions.InitTranslatorException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Andre Greiner-Petter
 */
public class TranslationWikidataTableGeneratorTest {
    private static TranslationWikidataTableGenerator t;

    @BeforeAll
    public static void setup() throws URISyntaxException, IOException, InitTranslatorException {
        URL dataURL = TranslationWikidataTableGeneratorTest.class.getResource("together.txt");
        URL csvURL = TranslationWikidataTableGeneratorTest.class.getResource("testQ.csv");
        URL mapSymbURL = TranslationWikidataTableGeneratorTest.class.getResource("MapleSymbolic");
        URL mapNumURL = TranslationWikidataTableGeneratorTest.class.getResource("MapleNumeric");
        URL matSymbURL = TranslationWikidataTableGeneratorTest.class.getResource("MathematicaSymbolic");
        URL matNumURL = TranslationWikidataTableGeneratorTest.class.getResource("MathematicaNumeric");
        URL matNumSymbSucURL = TranslationWikidataTableGeneratorTest.class.getResource("MathematicaNumericSymbolicSuccessful");

        t = new TranslationWikidataTableGenerator(
                Paths.get(dataURL.toURI()),
                Paths.get(mapSymbURL.toURI()),
                Paths.get(mapNumURL.toURI()),
                Paths.get(matSymbURL.toURI()),
                Paths.get(matNumURL.toURI()),
                Paths.get(matNumSymbSucURL.toURI()),
                Paths.get(csvURL.toURI())
        );

        t.setRange(0, 3);
        t.init();
    }

    @Test
    public void writeWikiTableTest(@TempDir Path tempDir) throws IOException, URISyntaxException {
        t.generateTable(tempDir);

        Path outputFile = tempDir.resolve("1.txt");
        assertTrue(Files.exists(outputFile));

        URL expectedOutputURL = TranslationWikidataTableGeneratorTest.class
                .getResource("expectedTableOutput.txt");
        Path expectedOutputPath = Paths.get(expectedOutputURL.toURI());
        List<String> expectedLines = Files.readAllLines(expectedOutputPath);
        assertLinesMatch(expectedLines, Files.readAllLines(outputFile));
    }
}
