package gov.nist.drmf.interpreter.semantic;

import gov.nist.drmf.interpreter.common.GlobalConstants;
import mlp.ParseException;
import mlp.PomParser;
import mlp.PomTaggedExpression;

import java.util.List;

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
        String output = "";
        try {
            output = parser.parse(math).toString(); //more code for testing
            List<PomTaggedExpression> comps = parser.parse(math).getComponents();
            for (int i = 0; i < comps.size(); i++) {
                System.out.println(comps.get(i));
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        System.out.println(output);
        return content; //for testing (as of now)
    }
}
