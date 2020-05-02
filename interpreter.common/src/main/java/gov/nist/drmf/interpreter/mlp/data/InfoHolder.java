package gov.nist.drmf.interpreter.mlp.data;

/**
 * @author Andre Greiner-Petter
 */
public class InfoHolder {
    private String casName, pattern;
    private Integer numVars;

    public InfoHolder() {
        casName = null;
        pattern = null;
        numVars = null;
    }

    public String getCasName() {
        return casName;
    }

    public void setCasName(String casName) {
        this.casName = casName;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public Integer getNumVars() {
        return numVars;
    }

    public void setNumVars(Integer numVars) {
        this.numVars = numVars;
    }
}
