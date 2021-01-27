package gov.nist.drmf.interpreter.common.eval;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Andre Greiner-Petter
 */
public class NumericCalculation implements Serializable {
    @JsonProperty("result")
    private TestResultType result;

    @JsonProperty("resultExpression")
    private String resultExpression;

    @JsonProperty("testValues")
    private Map<String, String> testValues;

    public NumericCalculation(){
        result = TestResultType.SKIPPED;
        testValues = new HashMap<>();
    }

    public NumericCalculation(TestResultType result){
        this.result = result;
        testValues = new HashMap<>();
    }

    public TestResultType getResult() {
        return result;
    }

    public void setResult(TestResultType result) {
        this.result = result;
    }

    public String getResultExpression() {
        return resultExpression;
    }

    public void setResultExpression(String resultExpression) {
        this.resultExpression = resultExpression;
    }

    public Map<String, String> getTestValues() {
        return testValues;
    }

    public void setTestValues(Map<String, String> testValues) {
        this.testValues = testValues;
    }
}
