package gov.nist.drmf.interpreter.generic.mlp.pojo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import gov.nist.drmf.interpreter.generic.macro.MacroBean;
import gov.nist.drmf.interpreter.generic.macro.MacroGenericSemanticEntry;

/**
 * 
 * @author Andre Greiner-Petter
 */
public class SemanticReplacementRule {
    private final MacroBean macro;

    private final MacroGenericSemanticEntry pattern;

    private final double score;

    public SemanticReplacementRule(
            MacroBean macro,
            MacroGenericSemanticEntry pattern,
            double score
    ) {
        this.macro = macro;
        this.pattern = pattern;
        this.score = score;
    }

    public MacroBean getMacro() {
        return macro;
    }

    public MacroGenericSemanticEntry getPattern() {
        return pattern;
    }

    public double getScore() {
        return score;
    }

    @JsonIgnore
    @Override
    public String toString() {
        return macro.getMetaInformation().getDescription() + " [" + score + "]: " + pattern.getGenericTex() + " -> " + pattern.getSemanticTex();
    }

    @JsonIgnore
    public String getWikitextTableString() {
        return String.format("| <math>%5.4f</math> || %s || <syntaxhighlight lang=tex inline>%s</syntaxhighlight> || -> || <syntaxhighlight lang=tex inline>%s</syntaxhighlight>\n|-",
                score, macro.getName(), pattern.getGenericTex(), pattern.getSemanticTex());
    }
}
