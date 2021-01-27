package gov.nist.drmf.interpreter.common.eval;

/**
 * @author Andre Greiner-Petter
 */
public interface ISymbolicTestCases {
    boolean isActivated();

    void setActivated(boolean activated);

    String buildCommand(String cmd);

    SymbolicTestIDs getID();

    default String getShortName() {
        return getID().getId();
    }

    String compactToString();

    String toString();
}
