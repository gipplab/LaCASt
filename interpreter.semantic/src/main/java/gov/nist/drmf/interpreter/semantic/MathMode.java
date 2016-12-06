package gov.nist.drmf.interpreter.semantic;

/**
 * Created by jrp4 on 11/29/16.
 */
public class MathMode extends LaTeXMode {

    private String delim;

    public MathMode(int start) {
        super(start);
    }

    public void setDelim(String delim) {
        this.delim = delim;
    }

    public String getDelim() {
        return delim;
    }

    public String makeReplacements(String content) {
        return delim + "a" + MathModeUtils.mathMode.get(delim); //for testing (as of now)
    }
}
