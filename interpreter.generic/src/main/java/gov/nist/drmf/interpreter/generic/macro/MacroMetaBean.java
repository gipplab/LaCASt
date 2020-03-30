package gov.nist.drmf.interpreter.generic.macro;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * @author Andre Greiner-Petter
 */
@SuppressWarnings("unused")
public class MacroMetaBean {
    /**
     * Textual description text
     */
    @JsonProperty("description")
    private String description;

    /**
     * Unique string of description
     */
    @JsonProperty("meaning")
    private String meaning;

    /**
     * OpenMath name
     */
    @JsonProperty("openMathID")
    private String openMathID;

    /**
     * Standard arguments and parameters
     */
    private MacroStandardArgumentsBean standardArguments;

    public MacroMetaBean() {}

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMeaning() {
        return meaning;
    }

    public void setMeaning(String meaning) {
        this.meaning = meaning;
    }

    public String getOpenMathID() {
        return openMathID;
    }

    public void setOpenMathID(String openMathID) {
        this.openMathID = openMathID;
    }

    @JsonGetter("standardArguments")
    public MacroStandardArgumentsBean getStandardArguments() {
        return standardArguments;
    }

    @JsonSetter("standardArguments")
    public void setStandardArguments(MacroStandardArgumentsBean standardArguments) {
        this.standardArguments = standardArguments;
    }
}
