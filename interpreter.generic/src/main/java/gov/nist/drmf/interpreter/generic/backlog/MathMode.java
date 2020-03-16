package gov.nist.drmf.interpreter.generic.backlog;

import gov.nist.drmf.interpreter.common.constants.GlobalPaths;
import mlp.ParseException;
import mlp.PomParser;
import mlp.PomTaggedExpression;

import java.util.List;

/**
 * Java class for representing LaTeX math mode
 */
public class MathMode extends LaTeXMode {

    /**
     * Delimiter that started the MathMode segment
     */
    private String delim;

    /**
     * Initializes MathMode object given the start index of math mode segment
     * @param start
     */
    public MathMode(int start) {
        super(start);
    }

    /**
     * Sets delim field
     * @param delim
     */
    public void setDelim(String delim) {
        this.delim = delim;
    }

    /**
     * Returns delim field
     * @return
     */
    public String getDelim() {
        return delim;
    }

    /**
     * Performs macro replacements on LaTeX
     * @param content
     * @return
     */
    public String makeReplacements(String content) {
        //TODO: finish implementing method
        String math = content.substring(delim.length(), content.length() - MathModeUtils.mathMode.get(delim).length());
        System.out.println(math);
        PomParser parser = new PomParser(GlobalPaths.PATH_REFERENCE_DATA);
        parser.addLexicons(GlobalPaths.DLMF_MACROS_LEXICON_NAME);
        String output = "";
        try {
            output = parser.parse(math).toString(); //more code for testing
            List<PomTaggedExpression> comps = parser.parse(math).getComponents();
            for (PomTaggedExpression comp : comps) {
                //System.out.println(comp);
            }
            System.out.println(output);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        //System.out.println(output);
        return content; //for testing (as of now)
    }
}
