package gov.nist.drmf.interpreter.common.eval;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import gov.nist.drmf.interpreter.common.text.TextUtility;

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

    @Override
    public String toString() {
        return toString(-1, "");
    }

    @JsonIgnore
    public String toString(int max, String msg) {
        StringBuilder sb = new StringBuilder("{");
        sb.append(resultExpression);
        sb.append(", {")
                .append(TextUtility.join( ", ", testValues.entrySet(), (e) -> e.getKey() + " := " + e.getValue(), max, msg))
                .append("}");
        return sb.append("}").toString();
    }
}
