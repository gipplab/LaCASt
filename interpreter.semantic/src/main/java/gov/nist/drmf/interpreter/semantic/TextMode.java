package gov.nist.drmf.interpreter.semantic;

/**
 * Created by jrp4 on 11/29/16.
 */
public class TextMode extends LaTeXMode {

    public TextMode(int start) {
        super(start);
    }

    public String makeReplacements(String content) {
        // placeholder
        return content;
    }
}
