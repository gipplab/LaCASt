package gov.nist.drmf.interpreter.evaluation.common;

import gov.nist.drmf.interpreter.common.tests.Resource;
import gov.nist.drmf.interpreter.mathematica.common.AssumeMathematicaAvailability;
import gov.nist.drmf.interpreter.mathematica.extension.MathematicaInterface;
import gov.nist.drmf.interpreter.mathematica.wrapper.MathLinkException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;

import java.util.LinkedList;

/**
 * @author Andre Greiner-Petter
 */
@AssumeMathematicaAvailability
@Disabled
public class MathematicaEvaluationDummy {

    private static MathematicaInterface mi;
    private static LinkedList<String> resultStrings;

    @BeforeAll
    static void setup() {
        mi = MathematicaInterface.getInstance();
        resultStrings = new LinkedList<>();
    }

    @Disabled
    @Resource("TestList.txt")
    public void test(String tests) throws MathLinkException {
        String[] testsA = tests.split("\n");
        for ( String test : testsA ) {
            test = test.replace("\\", "\\\\");
            String result = mi.evaluate( "ToExpression[\""+test+"\", TeXForm]" );
            resultStrings.addLast(result);
        }
    }

    @AfterAll
    static void finish() {
        for (String res : resultStrings) System.out.println(res);
    }
}
