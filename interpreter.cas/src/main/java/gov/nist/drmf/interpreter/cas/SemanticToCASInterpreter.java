package gov.nist.drmf.interpreter.cas;

import gov.nist.drmf.interpreter.cas.parser.SemanticLatexParser;
import gov.nist.drmf.interpreter.common.GlobalConstants;
import gov.nist.drmf.interpreter.common.symbols.Constants;
import gov.nist.drmf.interpreter.common.symbols.GreekLetters;

/**
 * The main class to translate semantic LaTeX
 * to a given CAS.
 *
 * @author Andre Greiner-Petter
 */
public class SemanticToCASInterpreter {

    public static void main(String[] args){
        GlobalConstants.CAS_KEY = GlobalConstants.KEY_MAPLE;
        GreekLetters.init();
        Constants.init();

        String test = "q*\\iunit+2-\\frac{\\sqrt[\\alpha]{1}}{2}";
        test = "18*\\JacobiP{\\cos{\\sqrt{2}}}{\\JacobiP{a}{b}{c}@{d}}{2}@{3}";
        //test = "\\iunit";
        //test = "\\JacobiP{\\alpha}{b}{c}@{\\cos(\\sqrt{x}\\frac{\\cos(\\cos(x\\frac{\\cos(x)}{\\sin(xz)}))}{\\tan(\\sin(\\sqrt[x]{absdsd}\\frac{\\cos(x)}{\\sin(xz)}))})}";

        SemanticLatexParser parser = new SemanticLatexParser();
        parser.init( GlobalConstants.PATH_REFERENCE_DATA );
        parser.parse(test);
        System.out.println(parser.getTranslatedExpression());
        System.out.println(parser.getExtraInformation());
        System.err.println(parser.getErrorMessage());
    }
}
