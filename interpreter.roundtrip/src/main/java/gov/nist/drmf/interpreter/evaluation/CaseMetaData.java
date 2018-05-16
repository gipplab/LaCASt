package gov.nist.drmf.interpreter.evaluation;

import gov.nist.drmf.interpreter.constraints.Constraints;

/**
 * @author Andre Greiner-Petter
 */
public class CaseMetaData {
    private Label label;
    private Constraints constraints;
    private int linenumber;

    public CaseMetaData(int linenumber, Label label, Constraints constraints){
        this.label = label;
        this.constraints = constraints;
        this.linenumber = linenumber;
    }

    public Label getLabel() {
        return label;
    }

    public Constraints getConstraints() {
        return constraints;
    }

    public int getLinenumber() {
        return linenumber;
    }
}
