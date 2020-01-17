package gov.nist.drmf.interpreter.evaluation.symbolic;

/**
 * @author Andre Greiner-Petter
 */
public interface ISymbolicTestCases {
    boolean isActivated();

    void setActivated(boolean activated);

    String buildCommand(String cmd);

    String getShortName();

    String compactToString();

    String toString();
}
