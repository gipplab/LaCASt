package gov.nist.drmf.interpreter.generic.mediawiki;

import gov.nist.drmf.interpreter.generic.mlp.pojo.MOIPresentations;

/**
 * @author Andre Greiner-Petter
 */
public class DefiningFormula {
    private double score;

    private String definingFormula;

    private String[] elements;

    public DefiningFormula(double score, MOIPresentations moi) {
        this.score = score;
        this.definingFormula = moi.getGenericLatex();
        this.elements = moi.getIngoingNodes().toArray(new String[0]);
    }

    public double getScore() {
        return score;
    }

    public String getDefiningFormula() {
        return definingFormula;
    }

    public String[] getElements() {
        return elements;
    }
}
