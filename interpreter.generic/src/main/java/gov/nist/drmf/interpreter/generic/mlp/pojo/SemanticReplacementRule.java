package gov.nist.drmf.interpreter.generic.mlp.pojo;

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
}
