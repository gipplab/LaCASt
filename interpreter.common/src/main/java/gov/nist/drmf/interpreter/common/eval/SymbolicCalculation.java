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
    @JsonProperty("result")
    private TestResultType result;

    @JsonProperty("testTitle")
    private String testTitle;

    @JsonProperty("testExpression")
    private String testExpression;

    @JsonProperty("resultExpression")
    private String resultExpression;

    @JsonProperty("wasAborted")
    private boolean wasAborted = false;

    @JsonProperty("conditionallySuccessful")
    private boolean wasConditionallySuccessful = false;

    public SymbolicCalculation(){}

    public TestResultType getResult() {
        return result;
    }

    @JsonGetter("wasAborted")
    public boolean wasAborted() {
        return wasAborted;
    }

    @JsonSetter("wasAborted")
    public void wasAborted(boolean wasAborted) {
        this.wasAborted = wasAborted;
    }

    public void setResult(TestResultType result) {
        this.result = result;
    }

    public String getTestExpression() {
        return testExpression;
    }

    public void setTestExpression(String testExpression) {
        this.testExpression = testExpression;
    }

    public String getResultExpression() {
        return resultExpression;
    }

    public void setResultExpression(String resultExpression) {
        this.resultExpression = resultExpression;
    }

    public String getTestTitle() {
        return testTitle;
    }

    public void setTestTitle(String testTitle) {
        this.testTitle = testTitle;
    }

    public boolean isWasConditionallySuccessful() {
        return wasConditionallySuccessful;
    }

    public void setWasConditionallySuccessful(boolean wasConditionallySuccessful) {
        this.wasConditionallySuccessful = wasConditionallySuccessful;
    }
}
