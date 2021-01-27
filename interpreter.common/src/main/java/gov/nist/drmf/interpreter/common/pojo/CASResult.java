package gov.nist.drmf.interpreter.common.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import gov.nist.drmf.interpreter.common.eval.NumericResult;
import gov.nist.drmf.interpreter.common.eval.SymbolicResult;

import java.io.Serializable;

/**
 * @author Andre Greiner-Petter
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "translation", "translationInformation", "numericResults", "symbolicResults"
})
public class CASResult implements Serializable {
    @JsonProperty("translation")
    private final String casRepresentation;

    @JsonProperty("translationInformation")
    private MetaTranslationInformation translationInformation;

    @JsonProperty("numericResults")
    private NumericResult numericResults;

    @JsonProperty("symbolicResults")
    private SymbolicResult symbolicResults;

    private CASResult() {
        casRepresentation = "";
    }

    public CASResult(String casRepresentation) {
        this.casRepresentation = casRepresentation;
    }

    public String getCasRepresentation() {
        return casRepresentation;
    }

    public NumericResult getNumericResults() {
        return numericResults;
    }

    public void setNumericResults(NumericResult numericResults) {
        this.numericResults = numericResults;
    }

    public SymbolicResult getSymbolicResults() {
        return symbolicResults;
    }

    public void setSymbolicResults(SymbolicResult symbolicResults) {
        this.symbolicResults = symbolicResults;
    }

    public MetaTranslationInformation getTranslationInformation() {
        return translationInformation;
    }

    public void setTranslationInformation(MetaTranslationInformation translationInformation) {
        this.translationInformation = translationInformation;
    }
}
