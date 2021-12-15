package gov.nist.drmf.interpreter.mathematica.core;

import gov.nist.drmf.interpreter.mathematica.common.AssumeMathematicaAvailability;
import gov.nist.drmf.interpreter.mathematica.common.Commands;
import gov.nist.drmf.interpreter.mathematica.wrapper.jlink.KernelLink;
import gov.nist.drmf.interpreter.mathematica.wrapper.MathLinkException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * This test is more of a playground than an actual testing unit.
 * Here you can quickly play around with the math kernel by using
 * {@link MathematicaInterface#getMathKernel()}.
 *
 * @author Andre Greiner-Petter
 */
@AssumeMathematicaAvailability
public class MathematicaKernelTests {
    private static final String JACOBIP = "JacobiP[n,\\[Alpha],\\[Beta],Cos[a \\[CapitalTheta]]]";
    private static final String JACOBIP_FULL_FORM = "JacobiP[n, \\[Alpha], \\[Beta], Cos[Times[a, \\[CapitalTheta]]]]";

    private static MathematicaInterface mi;

    @BeforeAll
    public static void setup() {
        mi = MathematicaInterface.getInstance();
    }

    @Test
    public void getFullFormTest() throws MathLinkException {
        KernelLink engine = mi.getMathKernel();
        String fullf = Commands.FULL_FORM.build(JACOBIP);
        String fullForm = engine.evaluateToOutputForm(fullf, 0);
        assertEquals(JACOBIP_FULL_FORM, fullForm, "Expected a different full form of JacobiP");
        System.out.println(fullForm);
    }

    @Test
    public void errorTest() throws MathLinkException {
        KernelLink engine = mi.getMathKernel();
        try {
            engine.evaluate("Cos[x]");
            engine.waitForAnswer();
            engine.getBoolean(); // error
            fail("No MathLinkException thrown? Impossible!");
        } catch ( MathLinkException mle ) {
            engine.clearError();
            engine.newPacket();
        }
    }

    @Test
    public void abortTest() throws MathLinkException {
        KernelLink engine = mi.getMathKernel();
        String test = "Integrate[Divide[1,t], {t, 1, Divide[1,z]}]";
        test = test + " - " + test;

        Thread abortThread = new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("Finished delay, call abort evaluation.");
            engine.abortEvaluation();
        });

        abortThread.start();
        try {
            engine.evaluate(test);
            engine.waitForAnswer();
            System.out.println(engine.getExpr());
            engine.newPacket();
        } catch (MathLinkException e) {
            e.printStackTrace();
            System.out.println(engine.getLastError());
            engine.clearError();
            engine.newPacket();
        }
    }

    @AfterAll
    public static void shutdown() {
        mi.shutdown();
    }

}
