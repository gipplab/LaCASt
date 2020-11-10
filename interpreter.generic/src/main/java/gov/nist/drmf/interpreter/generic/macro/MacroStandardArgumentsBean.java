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
    private LinkedList<String> standardOptionalParameters;

    /**
     * The standard parameters
     */
    private LinkedList<String> standardParameters;

    /**
     * The standard arguments
     */
    private LinkedList<String> standardVariables;

    public MacroStandardArgumentsBean() {
        standardOptionalParameters = new LinkedList<>();
        standardParameters = new LinkedList<>();
        standardVariables = new LinkedList<>();
    }

    @JsonIgnore
    public void setStandardParameters(int numOfOptionalParameter, String para) {
        LinkedList<String> argList = generateListOfArguments(para);
        this.standardOptionalParameters = new LinkedList<>(argList.subList(0, numOfOptionalParameter));
        this.standardParameters = new LinkedList<>(argList.subList(numOfOptionalParameter, argList.size()));
    }

    @JsonIgnore
    public void setStandardVariables(String args) {
        this.standardVariables = generateListOfArguments(args);
    }

    @JsonSetter("standardOptionalParameters")
    public void setStandardOptionalParameters(LinkedList<String> standardOptionalParameters) {
        this.standardOptionalParameters = standardOptionalParameters;
    }

    @JsonSetter("standardParameters")
    public void setStandardParameters(LinkedList<String> standardParameters) {
        this.standardParameters = standardParameters;
    }

    @JsonSetter("standardVariables")
    public void setStandardVariables(LinkedList<String> standardVariables) {
        this.standardVariables = standardVariables;
    }

    @JsonGetter("standardOptionalParameters")
    public LinkedList<String> getStandardOptionalParameters() {
        return standardOptionalParameters;
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
