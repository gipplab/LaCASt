package gov.nist.drmf.interpreter.generic.elasticsearch;

import gov.nist.drmf.interpreter.generic.macro.MacroBean;

/**
 * @author Andre Greiner-Petter
 */
public class MacroResult {
    private final double score;

    private final MacroBean macro;

    MacroResult(double score, MacroBean macro) {
        this.score = score;
        this.macro = macro;
    }

    public MacroBean getMacro() {
        return macro;
    }

    public double getScore() {
        return score;
    }

    @Override
    public String toString() {
        return macro.toString() + " [Score: " + score + "]";
    }
}
