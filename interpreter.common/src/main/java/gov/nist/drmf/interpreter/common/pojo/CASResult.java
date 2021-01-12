package gov.nist.drmf.interpreter.common.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import gov.nist.drmf.interpreter.common.eval.NumericResult;
import gov.nist.drmf.interpreter.common.eval.SymbolicResult;

/**
 * @author Andre Greiner-Petter
 */
public class CASResult {
    @JsonProperty("translation")
    private final String casRepresentation;

    @JsonProperty("numericResults")
    private NumericResult numericResults;

    @JsonProperty("symbolicResults")
    private SymbolicResult symbolicResults;

    private CASResult() {
        casRepresentation = "";
    }

    public CASResult(String casRepresentation) {
        this.casRepresentation = casRepresentation;
    }

    public String getCasRepresentation() {
        return casRepresentation;
    }

    public NumericResult getNumericResults() {
        return numericResults;
    }

    public void setNumericResults(NumericResult numericResults) {
        this.numericResults = numericResults;
    }

    public SymbolicResult getSymbolicResults() {
        return symbolicResults;
    }

    public void setSymbolicResults(SymbolicResult symbolicResults) {
        this.symbolicResults = symbolicResults;
    }
}
