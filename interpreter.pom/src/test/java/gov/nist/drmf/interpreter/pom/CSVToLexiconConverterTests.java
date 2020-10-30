package gov.nist.drmf.interpreter.pom;

import gov.nist.drmf.interpreter.common.constants.GlobalPaths;
import gov.nist.drmf.interpreter.pom.data.LexiconConverterConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Andre Greiner-Petter
 */
public class CSVToLexiconConverterTests {

    private static final String lexiconFileName = "DLMF-lexicon.txt";

    private static LexiconConverterConfig config;

    @TempDir
    static Path tempDir;

    private static Path resourceDir;

    private static CSVtoLexiconConverter converter;

    @BeforeAll
    public static void setup() throws Exception {
        String resourcePathStr = Objects.requireNonNull(
                CSVToLexiconConverterTests.class.getClassLoader().getResource("")
        ).getPath();
        resourceDir = Paths.get(resourcePathStr);

        config = new LexiconConverterConfig(
                resourceDir.resolve("csv/"),
                tempDir.resolve(lexiconFileName)
        );

        Path dlmf = Paths.get("DLMFMacro.csv");
        Path[] cas = new Path[]{
                Paths.get("CAS_Maple.csv"),
                Paths.get("CAS_Mathematica.csv")
        };

        converter = new CSVtoLexiconConverter(config, dlmf, cas);
    }

    @Test
    public void configTest() {
        LexiconConverterConfig defaultConfig = new LexiconConverterConfig();
        Path csvPath = defaultConfig.getCsvPath();
        Path macroPath = defaultConfig.getDlmfMacroLexiconPath();
        assertEquals( csvPath, GlobalPaths.PATH_REFERENCE_DATA_CSV );
        assertEquals( macroPath, GlobalPaths.DLMF_MACROS_LEXICON );
    }

    @Test
    public void generateLexiconTest() throws IOException {
        converter.generateLexiconFile();

        Path expectedLex = resourceDir.resolve("lexicon/Expected-"+lexiconFileName);
        Path generatedLex = tempDir.resolve(lexiconFileName);

        String expectedLexStr = Files.readString(expectedLex);
        String generatedLexStr = Files.readString(generatedLex);

        assertEquals(expectedLexStr, generatedLexStr);
    }

    @Test
    public void analyzeInputTest() {
        LinkedList<String> args = new LinkedList<>();
        CSVtoLexiconConverter.analyzeInput(args, "Maple");
        CSVtoLexiconConverter.analyzeInput(args, "CAS_Mathematica.csv");
        assertEquals( 2, args.size() );
        assertEquals( "CAS_Maple.csv", args.getFirst() );
        assertEquals( "CAS_Mathematica.csv", args.getLast() );
    }

    @Test
    public void analyzeInputAllTest() {
        LinkedList<String> args = new LinkedList<>();
        CSVtoLexiconConverter.analyzeInput(args, "--all");
        assertTrue( args.size() >= 2 );
        assertTrue( args.contains("CAS_Maple.csv") );
        assertTrue( args.contains("CAS_Mathematica.csv") );
    }

    @Test
    public void convertToPathsTest() {
        LinkedList<String> args = new LinkedList<>();
        args.add("CAS_Maple.csv");
        args.add("CAS_Mathematica.csv");

        Path[] p = CSVtoLexiconConverter.convertToPaths(args, null);
        assertEquals( 2, p.length );
        assertEquals( "CAS_Maple.csv", p[0].toString() );
        assertEquals( "CAS_Mathematica.csv", p[1].toString() );
    }

    @Test
    public void convertToPaths2Test() {
        LinkedList<String> args = new LinkedList<>();
        String[] progArgs = new String[]{"CAS_Maple.csv"};
        Path[] p = CSVtoLexiconConverter.convertToPaths(args, progArgs);
        assertEquals( 1, p.length );
        assertEquals( "CAS_Maple.csv", p[0].toString() );
    }
}
