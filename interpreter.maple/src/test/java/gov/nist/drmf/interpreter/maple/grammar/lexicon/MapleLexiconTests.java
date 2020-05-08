package gov.nist.drmf.interpreter.maple.grammar.lexicon;

import gov.nist.drmf.interpreter.common.constants.GlobalPaths;
import gov.nist.drmf.interpreter.mlp.MacrosLexicon;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * Created by AndreG-P on 01.03.2017.
 */
public class MapleLexiconTests {
    @BeforeAll
    public static void setup(){
        try {
            MacrosLexicon.init();
        } catch ( IOException ioe ){
            ioe.printStackTrace();
            fail("Cannot load lexicon.");
        }
    }

    @Test
    public void lexiconFactoryTest(){
        MapleLexiconFactory mff = new MapleLexiconFactory(
                new String[]{
                        "DLMF", "Maple", "Number of Variables",
                        "Maple-Link", "DLMF-Link"
                }
        );

        String[] test = new String[]{
                "\\acot@@{$0}",
                "arccot($0)",
                "1",
                "maple.html",
        };

        MapleFunction mf = mff.createMapleFunction( test );
        System.out.println( mf );
        assertEquals( "arccot", mf.getMAPLEName() );
        assertEquals( test[0], mf.getDLMFPattern() );
        assertTrue( 1 == mf.getNumberOfVariables() );
        assertEquals( test[3], mf.getMAPLELink() );
        assertEquals( "http://dlmf.nist.gov/4.23#SS2.p1", mf.getDLMFLink() );
    }

    @Test
    public void createLexicon() throws IOException {
        Path tester = GlobalPaths.PATH_REFERENCE_DATA_CSV.resolve("CAS_Maple.csv");
        MapleLexicon l = MapleLexiconFactory.createLexiconFromCSVFile( tester );
        assertNotNull(l);
        MapleFunction mf = l.getFunction("JacobiP", 4);
        assertNotNull(mf);
    }

    @Test
    public void littleWorkaroundTest() throws IOException {
        MapleLexicon.init();
    }

    @Test
    public void loadLexiconTest(){
        Path lex_path = GlobalPaths.PATH_REFERENCE_DATA_CAS_LEXICONS.resolve("Maple-functions-lexicon.txt");
        MapleLexicon l = MapleLexiconFactory.loadLexicon( lex_path );
        assertNotNull(l);
        MapleFunction mf = l.getFunction("JacobiP", 4);
        assertNotNull(mf);
    }
}
