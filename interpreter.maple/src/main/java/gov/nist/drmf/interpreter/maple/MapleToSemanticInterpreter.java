package gov.nist.drmf.interpreter.maple;

import com.maplesoft.externalcall.MapleException;
import gov.nist.drmf.interpreter.common.GlobalPaths;
import gov.nist.drmf.interpreter.common.Keys;
import gov.nist.drmf.interpreter.maple.grammar.TranslationFailures;
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
        System.setProperty( Keys.KEY_SYSTEM_LOGGING, GlobalPaths.PATH_LOGGING_CONFIG.toString() );

        /*
        LOG.trace("Test");
        LOG.debug("TEST");
        LOG.info("TEST");
        LOG.warn("TEST");
        LOG.error("TEST");
        LOG.fatal("TEST");
        */

        //*
        try {
            MapleInterface.init();
            MapleInterface mi = MapleInterface.getUniqueMapleInterface();

            String test = "(3.5+Catalan/2)^gamma";
//            test = "gamma+alpha^5-I^(x/5)+Catalan";
//            test = "x + x^2 + ((1-gamma)*x/2)^I";
//            test = "gamma + alpha^(5) +(1 * I)^(x *(1)/(5)) *(- 1)+ Catalan";
//            test = "1.4/((a+2)/(b^(Catalan/I)/(alpha*q^I*x/3)*alpha))";
//            test = "((x^a)^b)^c";

            String result = mi.translate(test);
            TranslationFailures tf = mi.getFailures();

            LOG.info( "Translated result: " + result );
            if ( !tf.isEmpty() )
                LOG.warn( "Internal Problems: " + mi.getFailures() );

        } catch ( MapleException | IOException me ){
            LOG.fatal( "Wasn't able to start the program.", me );
        }
        //*/
    }
}
