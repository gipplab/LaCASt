package gov.nist.drmf.interpreter.evaluation;

import gov.nist.drmf.interpreter.MapleTranslator;
import gov.nist.drmf.interpreter.constraints.Constraints;

import java.util.Arrays;

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

    public Relations getRelation() {
        return relation;
    }

    public String getConstraintVariables() {
        try {
            String[] vars = metaData.getConstraints().getSpecialConstraintVariables();
            vars = NumericalEvaluator.translateEach(vars);
            return Arrays.toString(vars);
        } catch ( NullPointerException npe ){
            return null;
        }
    }

    public String getConstraintValues() {
        try {
            String[] vals = metaData.getConstraints().getSpecialConstraintValues();
            return Arrays.toString(vals);
        } catch ( NullPointerException npe ){
            return null;
        }
    }

    public Constraints getConstraintObject(){
        return metaData.getConstraints();
    }

    public String getConstraints() {
        try {
            String[] cons = metaData.getConstraints().getTexConstraints();
            cons = NumericalEvaluator.translateEach(cons);
            return Arrays.toString(cons);
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

    public CaseMetaData getMetaData() {
        return metaData;
    }

    @Override
    public String toString(){
        String s = getLine() + ": " + LHS + " " + relation + " " + RHS + "; ";
//        s += metaData.getConstraints().toString();
        s += getDlmf();
        return s;
    }

}
