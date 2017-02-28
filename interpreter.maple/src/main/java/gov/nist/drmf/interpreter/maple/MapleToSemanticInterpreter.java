package gov.nist.drmf.interpreter.maple;

import com.maplesoft.externalcall.MapleException;
import gov.nist.drmf.interpreter.common.Keys;
import gov.nist.drmf.interpreter.maple.parser.MapleInterface;
import gov.nist.drmf.interpreter.maple.setup.Initializer;

import java.io.IOException;

/**
 * Created by AndreG-P on 17.02.2017.
 */
public class MapleToSemanticInterpreter {

    public static void main (String[] args){
        if ( Initializer.loadMapleNatives() )
            System.out.println("Loading Maple Natives!");
        else {
            System.out.println("Cannot load maple native directory.");
            return;
        }


        try {
            MapleInterface imaple = new MapleInterface(Keys.KEY_DLMF);
            imaple.init();
            String result = imaple.parse( "-2" );
            System.out.println("Translated to: " + result);
        } catch ( MapleException | IOException me ){
            System.out.println("Well, Maple-Exception... nice shit.");
            me.printStackTrace();
        }
    }

}
