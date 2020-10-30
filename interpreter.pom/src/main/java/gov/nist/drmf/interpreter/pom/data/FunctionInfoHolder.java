package gov.nist.drmf.interpreter.pom.data;

/**
 * @author Andre Greiner-Petter
 */
public class FunctionInfoHolder {
    private String casFunctionName, pattern;
    private int numVars;

    public FunctionInfoHolder() {
        casFunctionName = null;
        pattern = null;
        numVars = -1;
    }

    public String getCasFunctionName() {
        return casFunctionName;
    }

    public void setCasFunctionName(String casFunctionName) {
        this.casFunctionName = casFunctionName;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public int getNumVars() {
        return numVars;
    }

    public void setNumVars(int numVars) {
        this.numVars = numVars;
    }
}
