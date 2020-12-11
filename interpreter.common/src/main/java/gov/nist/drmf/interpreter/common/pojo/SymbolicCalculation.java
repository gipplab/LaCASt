package gov.nist.drmf.interpreter.common.pojo;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * @author Andre Greiner-Petter
 */
public class SymbolicCalculation {
    @JsonProperty("result")
    private String result;

    @JsonProperty("property")
    private String testProperty;

    public SymbolicCalculation(){}

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    @JsonGetter("property")
    public String getTestProperty() {
        return testProperty;
    }

    @JsonSetter("property")
    public void setTestProperty(String testProperty) {
        this.testProperty = testProperty;
    }
}
