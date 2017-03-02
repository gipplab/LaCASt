package gov.nist.drmf.interpreter.maple;

import com.maplesoft.externalcall.MapleException;
import gov.nist.drmf.interpreter.maple.grammar.lexicon.TranslationFailures;
import gov.nist.drmf.interpreter.maple.translation.MapleInterface;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

/**
 * Created by AndreG-P on 17.02.2017.
 */
public class MapleToSemanticInterpreter {
    public static final Logger LOG = LogManager.getLogger( MapleToSemanticInterpreter.class );

    public static void main (String[] args){
        System.setProperty(
                "log4j2.configurationFile",
                "interpreter.maple/src/main/resources/log4j2.xml" );

        //*
        try {
            MapleInterface.init();
            MapleInterface mi = MapleInterface.getUniqueMapleInterface();

            String test = "(infinity+Catalan/2)^gamma";
//            test = "gamma+alpha^5-I^(x/5)+Catalan";
//            test = "x + x^2 + ((1-gamma)*x/2)^I";
//            test = "gamma + alpha^(5) +(1 * I)^(x *(1)/(5)) *(- 1)+ Catalan";
//            test = "1/((a+2)/(b^(Catalan/I)/(alpha*q^I*x/3)*alpha))";
//            test = "((x^a)^b)^c";

            String result = mi.translate(test);
            TranslationFailures tf = mi.getFailures();
            System.out.println("Result:   " + result);
            System.out.println("Failures: " + tf.getFailures());
        } catch ( MapleException | IOException me ){
            LOG.error( "Wasn't able to start the program.", me );
        }
        //*/
    }
}
