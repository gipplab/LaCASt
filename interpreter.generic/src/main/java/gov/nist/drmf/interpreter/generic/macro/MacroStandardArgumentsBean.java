package gov.nist.drmf.interpreter.generic.macro;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.LinkedList;

import static gov.nist.drmf.interpreter.generic.macro.MacroHelper.generateListOfArguments;

/**
 * @author Andre Greiner-Petter
 */
@SuppressWarnings("unused")
public class MacroStandardArgumentsBean {
    /**
     * The standard parameters
     */
    private LinkedList<String> standardParameters;

    /**
     * The standard arguments
     */
    private LinkedList<String> standardVariables;

    public MacroStandardArgumentsBean() {
        standardParameters = new LinkedList<>();
        standardVariables = new LinkedList<>();
    }

    @JsonIgnore
    public void setStandardParameters(String para) {
        this.standardParameters = generateListOfArguments(para);
    }

    @JsonIgnore
    public void setStandardVariables(String args) {
        this.standardVariables = generateListOfArguments(args);
    }

    @JsonSetter("standardParameters")
    public void setStandardParameters(LinkedList<String> standardParameters) {
        this.standardParameters = standardParameters;
    }

    @JsonSetter("standardVariables")
    public void setStandardVariables(LinkedList<String> standardVariables) {
        this.standardVariables = standardVariables;
    }

    @JsonGetter("standardParameters")
    public LinkedList<String> getStandardParameters() {
        return standardParameters;
    }

    @JsonGetter("standardVariables")
    public LinkedList<String> getStandardVariables() {
        return standardVariables;
    }
}
