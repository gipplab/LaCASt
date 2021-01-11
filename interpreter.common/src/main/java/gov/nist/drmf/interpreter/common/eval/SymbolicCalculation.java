package gov.nist.drmf.interpreter.common.eval;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.io.Serializable;

/**
 * @author Andre Greiner-Petter
 */
public class SymbolicCalculation implements Serializable {
    @JsonProperty("property")
    private String testProperty;

    @JsonProperty("result")
    private String result;

    @JsonProperty("wasAborted")
    private boolean wasAborted = false;

    @JsonProperty("wasConditionallySuccessful")
    private boolean wasConditionallySuccessful = false;

    @JsonIgnore
    private boolean wasSuccessful;

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

    @JsonGetter("wasAborted")
    public boolean wasAborted() {
        return wasAborted;
    }

    @JsonSetter("wasAborted")
    public void wasAborted(boolean wasAborted) {
        this.wasAborted = wasAborted;
    }

    public boolean isWasConditionallySuccessful() {
        return wasConditionallySuccessful;
    }

    public void setWasConditionallySuccessful(boolean wasConditionallySuccessful) {
        this.wasConditionallySuccessful = wasConditionallySuccessful;
    }

    public boolean wasSuccessful() {
        return wasSuccessful;
    }

    public void wasSuccessful(boolean wasSuccessful) {
        this.wasSuccessful = wasSuccessful;
    }
}
