package gov.nist.drmf.interpreter.maple;

import gov.nist.drmf.interpreter.common.InformationLogger;
import gov.nist.drmf.interpreter.common.constants.GlobalPaths;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.maple.grammar.TranslationFailures;
import gov.nist.drmf.interpreter.maple.translation.MapleTranslator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Scanner;

/**
 * Created by AndreG-P on 17.02.2017.
 */
public class MapleToSemanticInterpreter {
    public static final Logger LOG = LogManager.getLogger(MapleToSemanticInterpreter.class);

    public static void main(String[] args) {
//        System.setProperty(Keys.KEY_SYSTEM_LOGGING, GlobalPaths.PATH_LOGGING_CONFIG.toString());

        MapleTranslator mi = MapleTranslator.getDefaultInstance();
        if (mi == null) {
            LOG.error("Unable to start maple interface");
            return;
        }

        String test = "sin(x) > exp(x)";
//            test = "gamma+alpha^5-I^(x/5)+Catalan";
//            test = "x + x^2 + ((1-gamma)*x/2)^I";
//            test = "gamma + alpha^(5) +(1 * I)^(x *(1)/(5)) *(- 1)+ Catalan";
//            test = "1.4/((a+2)/(b^(Catalan/I)/(alpha*q^I*x/3)*alpha))";
//            test = "((x^a)^b)^c";

        System.out.println("Please enter a Maple expression in 1-D representation" +
                System.lineSeparator() +
                "(without ;):");
        Scanner console = new Scanner(System.in);
        test = console.nextLine();

        //Algebraic a = mi.evaluateExpression( "ToInert('sin(x)');" );
        //System.out.println(a.toString());

        String result = mi.translate(test);
        TranslationFailures tf = mi.getFailures();
        InformationLogger il = mi.getInfos();

        if (!tf.isEmpty()) {
            System.out.println("Error in translation process:");
            System.out.println(tf);
        }

        System.out.println("Translated expression:");
        System.out.println(result);

        System.out.println("Additional Information:");
        System.out.println(il.toString());

        LOG.info("Translated result: " + result);
        if (!tf.isEmpty())
            LOG.warn("Internal Problems: " + mi.getFailures());

    }
}
