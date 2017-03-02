package gov.nist.drmf.interpreter.maple.translation;

import gov.nist.drmf.interpreter.maple.grammar.lexicon.MapleFunction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by AndreG-P on 01.03.2017.
 */
public class MapleLexiconTests {

    @Test
    public void MapleFunctionTest(){
        MapleFunction mf = new MapleFunction(
                "arctan", "\\ph@@{$1+($0)*\\iunit}",
                "maple-link", "dlmf-link",
                2
        );
        String s = mf.replacePlaceHolders( new String[]{"1", "2"} );
        assertEquals( "\\ph@@{2+(1)*\\iunit}", s );
    }

}
