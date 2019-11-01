package gov.nist.drmf.interpreter.cas.translation.components.cases;

/**
 * @author Andre Greiner-Petter
 */
public interface ExceptionalTestCase {
    String getTitle();

    String getTex();

    Class getException();
}
