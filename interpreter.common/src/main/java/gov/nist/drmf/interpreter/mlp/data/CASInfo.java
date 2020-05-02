package gov.nist.drmf.interpreter.mlp.data;

/**
 * Created by AndreG-P on 04.04.2017.
 */
public class CASInfo {
    private String link = "",
            constraints = "",
            branch_cuts = "",
            extra_package = "";

    public CASInfo() {}

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getConstraints() {
        return constraints;
    }

    public void setConstraints(String constraints) {
        this.constraints = constraints;
    }

    public String getBranch_cuts() {
        return branch_cuts;
    }

    public void setBranch_cuts(String branch_cuts) {
        this.branch_cuts = branch_cuts;
    }

    public String getExtra_package() {
        return extra_package;
    }

    public void setExtra_package(String extra_package) {
        this.extra_package = extra_package;
    }
}
