package gov.nist.drmf.interpreter.common.tests;

/**
 * @author Andre Greiner-Petter
 */
public interface ExceptionalTestCase extends TestCase {
    Class<?> getException();
}
