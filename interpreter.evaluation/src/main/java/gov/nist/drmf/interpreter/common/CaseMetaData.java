package gov.nist.drmf.interpreter.common;

import gov.nist.drmf.interpreter.constraints.Constraints;

import java.util.LinkedList;

/**
 * @author Andre Greiner-Petter
 */
public class CaseMetaData {
    private Label label;
    private Constraints constraints;

    private LinkedList<SymbolTag> symbolsUsed;

    private int linenumber;

    public CaseMetaData(int linenumber, Label label, Constraints constraints, LinkedList<SymbolTag> symbolsUsed){
        this.label = label;
        this.constraints = constraints;
        this.linenumber = linenumber;
        this.symbolsUsed = symbolsUsed;
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

    public void deleteConstraints() {
        this.constraints = null;
    }

    public LinkedList<SymbolTag> getSymbolsUsed() {
        return symbolsUsed;
    }
}
