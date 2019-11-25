package gov.nist.drmf.interpreter.evaluation;

import gov.nist.drmf.interpreter.constraints.Constraints;

/**
 * @author Andre Greiner-Petter
 */
public class CaseMetaData {
    private Label label;
    private Constraints constraints;
    private String code;
    private int linenumber;

    public CaseMetaData(int linenumber, Label label, Constraints constraints, String code){
        this.label = label;
        this.constraints = constraints;
        this.linenumber = linenumber;
        this.code = code;
    }

    public Label getLabel() {
        return label;
    }

    public Constraints getConstraints() {
        return constraints;
    }

    public String getCode() {
        return code;
    }

    public int getLinenumber() {
        return linenumber;
    }

    public void deleteConstraints() {
        this.constraints = null;
    }
}
