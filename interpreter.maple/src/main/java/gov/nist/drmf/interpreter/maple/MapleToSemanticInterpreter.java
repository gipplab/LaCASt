package gov.nist.drmf.interpreter.maple;

import com.maplesoft.externalcall.MapleException;
import com.maplesoft.openmaple.Engine;
import com.maplesoft.openmaple.EngineCallBacksDefault;
import gov.nist.drmf.interpreter.maple.setup.Initializer;

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
            Engine e = new Engine( new String[]{"java"}, new EngineCallBacksDefault(), null, null );
        } catch ( MapleException me ){
            System.out.println("Well, Maple-Exception... nice shit.");
            me.printStackTrace();
        }

        System.out.println("MUHAHA");
    }

}
