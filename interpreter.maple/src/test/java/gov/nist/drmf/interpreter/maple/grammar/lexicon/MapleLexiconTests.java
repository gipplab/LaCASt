package gov.nist.drmf.interpreter.maple.grammar.lexicon;

import gov.nist.drmf.interpreter.common.GlobalPaths;
import gov.nist.drmf.interpreter.mlp.extensions.MacrosLexicon;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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
        assertEquals( "dlmf.nist.gov/4.23#SS2.p1", mf.getDLMFLink() );
    }

    @Test
    public void createLexicon(){
        Path tester = GlobalPaths.PATH_REFERENCE_DATA_CSV.resolve("CAS_Maple_test.csv");
        try {
            MapleLexicon ml = MapleLexiconFactory.createLexiconFromCSVFile( tester );
        } catch ( IOException ioe ){
            ioe.printStackTrace();
        }
    }

    @Test
    public void littleWorkaroundTest(){
        try {
            MapleLexicon.init();
        } catch ( IOException ioe ){
            ioe.printStackTrace();
        }
    }

    @Test
    public void loadLexiconTest(){
        Path lex_path = GlobalPaths.PATH_REFERENCE_DATA.resolve("MapleLexiconTest.txt");
        MapleLexicon l = MapleLexiconFactory.loadLexicon( lex_path );
    }
}
