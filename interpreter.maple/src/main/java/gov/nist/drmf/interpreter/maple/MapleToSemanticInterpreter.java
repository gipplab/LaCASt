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
        try {
            MapleInterface imaple = new MapleInterface();
            imaple.init();

            String test = "(infinity+Catalan/2)^gamma";
            //test = "gamma+alpha^5-I^(x/5)+Catalan";
            //test = "x + x^2 + ((1-gamma)*x/2)^I";
            //test = "gamma + alpha^(5) +(1 * I)^(x *(1)/(5)) *(- 1)+ Catalan";
            test = "1/((a+2)/(b^(Catalan/I)/(alpha*q^I*x/3)*alpha))";
            //test = "((x^a)^b)^c";

            String result = imaple.parse(test);

            System.out.println("Translated to: " + result);
            System.out.println("ErrorLOG: " + imaple.getInternalErrorLog());
        } catch ( MapleException | IOException me ){
            System.out.println("Well, Maple-Exception... nice shit.");
            me.printStackTrace();
        }
    }

}
