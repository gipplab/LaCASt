package gov.nist.drmf.interpreter.common.eval;

import gov.nist.drmf.interpreter.common.replacements.DLMFConditionalReplacementImpl;

/**
 * @author Andre Greiner-Petter
 */
public class Label {
    private String tex;
    private String hyperlink;

    public Label(String tex){
        this.tex = tex;
        this.hyperlink = tex;
    }

    public String getTex() {
        return tex;
    }

    public String getHyperlink() {
        return hyperlink;
    }

    public String getLabel() {
        return DLMFConditionalReplacementImpl.extractEquationLabelFromURL(hyperlink);
    }
}
