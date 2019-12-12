package gov.nist.drmf.interpreter.evaluation;

import gov.nist.drmf.interpreter.constraints.Constraints;
import gov.nist.drmf.interpreter.constraints.IConstraintTranslator;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Andre Greiner-Petter
 */
public class Case {

    private String LHS, RHS;

    private Relations relation;
    private CaseMetaData metaData;

    public Case( String LHS, String RHS, Relations relation, CaseMetaData metaData ){
        this.LHS = LHS;
        this.RHS = RHS;
        this.relation = relation;
        this.metaData = metaData;
    }

    public String getLHS() {
        return LHS;
    }

    public String getRHS() {
        return RHS;
    }

    public int getLine() {
        return metaData.getLinenumber();
    }

    public String getDlmf() {
        if ( metaData.getLabel() == null ) return null;
        return metaData.getLabel().getHyperlink();
    }

    public String getEquationLabel() {
        if ( metaData.getLabel() == null ) return null;
        return metaData.getLabel().getLabel();
    }

    public Relations getRelation() {
        return relation;
    }

    public List<String> getConstraintVariables(IConstraintTranslator ae, String label) {
        try {
            String[] vars = metaData.getConstraints().getSpecialConstraintVariables();
            vars = ae.translateEachConstraint(vars, label);
            return new LinkedList<>(Arrays.asList(vars));
        } catch ( NullPointerException npe ){
            return null;
        }
    }

    public List<String> getConstraintValues() {
        try {
            String[] vals = metaData.getConstraints().getSpecialConstraintValues();
            return new LinkedList<>(Arrays.asList(vals));
        } catch ( NullPointerException npe ){
            return null;
        }
    }

    public Constraints getConstraintObject(){
        return metaData.getConstraints();
    }

    public List<String> getConstraints(IConstraintTranslator ae, String label ) {
        try {
            String[] cons = metaData.getConstraints().getTexConstraints();
            cons = ae.translateEachConstraint(cons, label);
            return new LinkedList<>(Arrays.asList(cons));
        } catch ( NullPointerException npe ){
            return null;
        }
    }

    public boolean isEquation(){
        return relation.equals(Relations.EQUAL);
    }

    public String specialValueInfo(){
        Constraints con = metaData.getConstraints();
        if ( con == null ) return null;
        return con.toString();
    }

    public String getRawConstraint() {
        if ( metaData.getConstraints() != null )
            return Arrays.toString(metaData.getConstraints().getTexConstraints());
        else return null;
    }

    public void removeConstraint() {
        metaData.deleteConstraints();
    }

    public CaseMetaData getMetaData() {
        return metaData;
    }

    @Override
    public String toString(){
        String s = getLine() + ": " + LHS + " " + relation + " " + RHS + "; ";
        s += getDlmf();
        return s;
    }

}
