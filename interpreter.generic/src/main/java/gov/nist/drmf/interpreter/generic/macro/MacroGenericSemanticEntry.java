package gov.nist.drmf.interpreter.generic.macro;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * @author Andre Greiner-Petter
 */
public class MacroGenericSemanticEntry {
    @JsonProperty("genericTeX")
    private String genericTex;

    @JsonProperty("semanticTeX")
    private String semanticTex;

    @JsonProperty("score")
    private double score;

    public MacroGenericSemanticEntry() {
        this("", "", 0.0);
    }

    public MacroGenericSemanticEntry(String genericTex, String semanticTex, double score) {
        this.genericTex = genericTex;
        this.semanticTex = semanticTex;
        this.score = score;
    }

    @JsonGetter("genericTeX")
    public String getGenericTex() {
        return genericTex;
    }

    @JsonSetter("genericTeX")
    public void setGenericTex(String genericTex) {
        this.genericTex = genericTex;
    }

    @JsonGetter("semanticTeX")
    public String getSemanticTex() {
        return semanticTex;
    }

    @JsonSetter("semanticTeX")
    public void setSemanticTex(String semanticTex) {
        this.semanticTex = semanticTex;
    }

    @JsonGetter("score")
    public double getScore() {
        return score;
    }

    @JsonSetter("score")
    public void setScore(double score) {
        this.score = score;
    }

    @JsonIgnore
    @Override
    public boolean equals(Object o) {
        if ( o != null && !(o instanceof MacroGenericSemanticEntry) ) return false;
        MacroGenericSemanticEntry ref = (MacroGenericSemanticEntry) o;
        return genericTex.equals(ref.genericTex) && semanticTex.equals(ref.semanticTex) && score == ref.score;
    }

    @JsonIgnore
    @Override
    public String toString() {
        return "(" + genericTex + ", " + semanticTex + ", " + score + ")";
    }
}
