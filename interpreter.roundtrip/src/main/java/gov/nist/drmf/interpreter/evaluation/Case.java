package gov.nist.drmf.interpreter.evaluation;

/**
 * @author Andre Greiner-Petter
 */
public class Case {

    private String LHS, RHS;
    private int line;
    private String assumption;
    private String dlmf;

    private Relations relation;

    public Case( String LHS, String RHS, Relations relation, String assumption, String dlmf, int line ){
        this.LHS = LHS;
        this.RHS = RHS;
        this.assumption = assumption;
        this.dlmf = dlmf;
        this.line = line;
        this.relation = relation;
    }

    public String getLHS() {
        return LHS;
    }

    public String getRHS() {
        return RHS;
    }

    public int getLine() {
        return line;
    }

    public String getAssumption() {
        return assumption;
    }

    public String getDlmf() {
        return dlmf;
    }

    public Relations getRelation() {
        return relation;
    }

    public boolean isEquation(){
        return relation.equals(Relations.EQUAL);
    }

    @Override
    public String toString(){
        return line + ": " + LHS + "=" + RHS + " CONSTR=" + assumption + "; " + dlmf;
    }

}
