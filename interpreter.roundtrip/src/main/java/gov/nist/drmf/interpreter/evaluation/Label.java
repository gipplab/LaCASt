package gov.nist.drmf.interpreter.evaluation;

/**
 * @author Andre Greiner-Petter
 */
public class Label {

    private String tex;
    private String hyperlink;

    public Label(String tex){
        this.tex = tex;
        this.hyperlink = DLMFLinker.getLinkerInstance().getLink(tex);
    }

    public String getTex() {
        return tex;
    }

    public String getHyperlink() {
        return hyperlink;
    }
}
