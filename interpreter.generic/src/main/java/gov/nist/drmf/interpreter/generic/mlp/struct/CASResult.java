package gov.nist.drmf.interpreter.generic.mlp.struct;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Andre Greiner-Petter
 */
public class CASResult {
    @JsonProperty("translation")
    private final String casRepresentation;

    @JsonProperty("numericResults")
    private final Map<String, String> numericResults;

    @JsonProperty("symbolicResults")
    private final Map<String, String> symbolicResults;

    public CASResult(String casRepresentation) {
        this.casRepresentation = casRepresentation;
        numericResults = new HashMap<>();
        symbolicResults = new HashMap<>();
    }

    public String getCasRepresentation() {
        return casRepresentation;
    }

    public Map<String, String> getNumericResults() {
        return numericResults;
    }

    public Map<String, String> getSymbolicResults() {
        return symbolicResults;
    }

    public void addNumericResult(String testValues, String result){
        numericResults.put(testValues, result);
    }

    public void addSymbolicResult(String type, String result) {
        symbolicResults.put(type, result);
    }
}
