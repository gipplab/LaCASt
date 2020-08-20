package gov.nist.drmf.interpreter.common.tests;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Andre Greiner-Petter
 */
public class TranslationTestCase {
    @JsonProperty("name")
    private String name;

    @JsonProperty("LaTeX")
    private String latex;

    @JsonProperty("DLMF")
    private String label;

    @JsonIgnore
    private Map<String, String> translationMap;

    private TranslationTestCase() {
        translationMap = new HashMap<>();
    }

    @JsonAnySetter
    void setData(String key, String value) {
        switch (key) {
            case "name":
                this.name = value;
                break;
            case "LaTeX":
                this.latex = value;
                break;
            case "DLMF":
                this.label = value;
                break;
            default:
                translationMap.put(key, value);
        }
    }

    public String getName() {
        return name;
    }

    public String getLatex() {
        return latex;
    }

    /**
     * Returns the DLMF label of the current test case if available.
     * If there is on DLMF label, null will be returned.
     * @return the DLMF label or null
     */
    public String getLabel() {
        return label;
    }

    /**
     * Returns the CAS translation of an object
     * @param cas the key of the computer algebra system, e.g. Maple or Mathematica
     * @return the cas translation or null if not available
     */
    public String getCASTranslation(String cas) {
        return translationMap.get(cas);
    }

    @Override
    public String toString() {
        if ( label != null && !label.isBlank() )
            return "DLMF:" + label + " (" + name + ")";
        return name;
    }
}
