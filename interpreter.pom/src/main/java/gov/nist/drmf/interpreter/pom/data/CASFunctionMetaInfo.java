package gov.nist.drmf.interpreter.pom.data;

/**
 * Created by AndreG-P on 04.04.2017.
 */
public class CASFunctionMetaInfo {
    private String link = "",
            constraints = "",
            branchCuts = "";

    public CASFunctionMetaInfo() {}

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        if ( link != null && link.startsWith("https://") ) link = link.substring("https://".length());
        this.link = link;
    }

    public String getConstraints() {
        return constraints;
    }

    public void setConstraints(String constraints) {
        this.constraints = constraints;
    }

    public String getBranchCuts() {
        return branchCuts;
    }

    public void setBranchCuts(String branch_cuts) {
        this.branchCuts = branch_cuts;
    }
}
