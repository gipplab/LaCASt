package gov.nist.drmf.interpreter.common.eval;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Andre Greiner-Petter
 */
public class NumericCalculation implements Serializable {
    @JsonProperty("result")
    private String result;

    @JsonProperty("testValues")
    private Map<String, String> testValues;

    public NumericCalculation(){
        testValues = new HashMap<>();
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public Map<String, String> getTestValues() {
        return testValues;
    }

    public void setTestValues(Map<String, String> testValues) {
        this.testValues = testValues;
    }
}
