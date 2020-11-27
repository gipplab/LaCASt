package gov.nist.drmf.interpreter.generic.macro;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.drmf.interpreter.pom.SemanticMLPWrapper;
import gov.nist.drmf.interpreter.pom.common.meta.AssumeMLPAvailability;
import mlp.ParseException;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Andre Greiner-Petter
 */
@AssumeMLPAvailability
public class MacroDistributionAnalyzerTests {
    private static final SemanticMLPWrapper mlp = SemanticMLPWrapper.getStandardInstance();

    @Test
    void checkOptionalArgsCounter() throws ParseException {
        MacroDistributionAnalyzer analyzer = new MacroDistributionAnalyzer();
        analyzer.analyze( mlp.parse("\\FerrersP[m]{n+2k}@{x}") );
        MacroCounter counter = analyzer.getMacroCounter("\\FerrersP");
        assertEquals("\\FerrersP", counter.getMacro());
        assertEquals(1, counter.getMacroCounter());
        assertEquals(1, counter.getOptionalArgumentCounter());
        assertEquals(1, counter.getNumberOfAtsCounter(1));
        assertEquals(1, counter.getAtCounter().size());
    }

    @Test
    void checkFerrersPTwiceCounter() throws ParseException {
        MacroDistributionAnalyzer analyzer = new MacroDistributionAnalyzer();
        analyzer.analyze( mlp.parse("\\FerrersP[m]{n+2k}^2@{x} + \\FerrersP{n+2k^2}@@{x}") );
        MacroCounter counter = analyzer.getMacroCounter("\\FerrersP");
        assertEquals("\\FerrersP", counter.getMacro());
        assertEquals(2, counter.getMacroCounter());
        assertEquals(1, counter.getOptionalArgumentCounter());
        assertEquals(0, counter.getNumberOfAtsCounter(0));
        assertEquals(1, counter.getNumberOfAtsCounter(1));
        assertEquals(1, counter.getNumberOfAtsCounter(2));
        assertEquals(2, counter.getAtCounter().size());

        assertEquals(0, counter.getScore(true, 0));
        assertEquals(0.25, counter.getScore(true, 1));

        // that sounds a bit weird at first but optional parameter and @s are not connected
        // simply said, in 1 of 2 cases, we have 2 @, and in 1 of two cases we have an optional
        // argument... hence its 0.5*0.5=0.25
        // Do not mix it up with the meaning: how often does an optional argument appear with 2 @s...
        // we cannot answer that
        assertEquals(0.25, counter.getScore(true, 2));

        assertEquals(0, counter.getScore(false, 0));
        assertEquals(0, counter.getScore(false, 1));
        assertEquals(0.25, counter.getScore(false, 2));
    }

    @Test
    void serializerCheck() throws IOException {
        String jacobiSerialized = MacroDefinitionTests.readResource("JacobipolyPDistributions.json");
        ObjectMapper mapper = new ObjectMapper();
        MacroCounter counter = mapper.readValue( jacobiSerialized, MacroCounter.class );
        assertNotNull(counter);
        assertEquals(136, counter.getMacroCounter());
        assertEquals(0, counter.getOptionalArgumentCounter());
        assertEquals(0, counter.getNumberOfAtsCounter(0));
        assertEquals(136, counter.getNumberOfAtsCounter(1));
        assertEquals(0, counter.getScore(true, 1));
        assertEquals(0, counter.getScore(false, 0));
        assertEquals(1, counter.getScore(false, 1));
    }

    @Test
    void standardInstanceTest() throws IOException {
        MacroDistributionAnalyzer analyzer = MacroDistributionAnalyzer.getStandardInstance();
        MacroCounter counter = analyzer.getMacroCounter( "\\JacobipolyP" );
        assertNotNull(counter);
        assertEquals(136, counter.getMacroCounter());
        assertEquals(0, counter.getOptionalArgumentCounter());
        assertEquals(0, counter.getNumberOfAtsCounter(0));
        assertEquals(136, counter.getNumberOfAtsCounter(1));
        assertEquals(0, counter.getScore(true, 1));
        assertEquals(0, counter.getScore(false, 0));
        assertEquals(1, counter.getScore(false, 1));
    }
}
