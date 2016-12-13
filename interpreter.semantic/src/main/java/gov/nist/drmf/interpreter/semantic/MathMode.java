package gov.nist.drmf.interpreter.semantic;

import gov.nist.drmf.interpreter.common.GlobalConstants;
import mlp.ParseException;
import mlp.PomParser;

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
        String math = content.substring(delim.length(), content.length() - MathModeUtils.mathMode.get(delim).length());
        System.out.println(math);
        PomParser parser = new PomParser(GlobalConstants.PATH_REFERENCE_DATA);
        parser.addLexicons(GlobalConstants.DLMF_MACROS_LEXICON_NAME);
        try {
            System.out.println(parser.parse(math));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return content; //for testing (as of now)
    }
}
