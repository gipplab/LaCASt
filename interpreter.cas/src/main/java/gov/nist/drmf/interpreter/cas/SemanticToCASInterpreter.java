package gov.nist.drmf.interpreter.cas;

import gov.nist.drmf.interpreter.cas.logging.InformationLogger;
import gov.nist.drmf.interpreter.cas.parser.SemanticLatexParser;
import gov.nist.drmf.interpreter.common.GlobalConstants;
import gov.nist.drmf.interpreter.common.Keys;
import gov.nist.drmf.interpreter.common.symbols.BasicFunctionsTranslator;
import gov.nist.drmf.interpreter.common.symbols.Constants;
import gov.nist.drmf.interpreter.common.symbols.GreekLetters;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The main class to translate semantic LaTeX
 * to a given CAS.
 *
 * @author Andre Greiner-Petter
 */
public class SemanticToCASInterpreter {
    public static final String KEY = Keys.KEY_MAPLE;

    public static final GreekLetters GREEK = new GreekLetters(Keys.KEY_LATEX, KEY);
    public static final Constants CONSTANTS = new Constants(Keys.KEY_DLMF, KEY);
    public static final BasicFunctionsTranslator FUNCTIONS = new BasicFunctionsTranslator( KEY );

    public static final InformationLogger INFO_LOG = new InformationLogger();
    public static final Logger ERROR_LOG = Logger.getLogger(SemanticToCASInterpreter.class.getName());

    public static void main(String[] args){
        Keys.CAS_KEY = KEY;

        ERROR_LOG.setLevel(Level.WARNING);

        GREEK.init();
        CONSTANTS.init();
        FUNCTIONS.init();

        String test = "q*\\iunit+2-\\frac{\\sqrt[\\alpha]{\\cpi}}{2\\JacobiP{i}{\\beta}{2}@{12.6}}";
//        test = "\\JacobiP{\\iunit}{b}{c}@{d}";
//        test = "18*\\JacobiP{\\cos{\\sqrt{2}}}{\\JacobiP{\\iunit}{b}{c}@{d}}{2}@{3}";
        test = "x^{\\JacobiP{\\iunit}{b}{c}@{d}}";

//        test = "\\JacobiP{\\alpha}{b}{c}@{\\cos(\\sqrt{x}\\frac{ \\cos(\\cos(x\\frac{\\cos(x)}{\\sin(xz)}))}{\\tan(\\sin(\\sqrt[x]{absdsd}\\frac{\\cos(x)}{\\sin(xz)}))})}";
//        test = "\\JacobiP{\\alpha\\sqrt[3]{x}\\sin(x\\alpha xyz)\\sqrt[2]{3}}{b\\frac{1}{\\pi}}{1+0\\cos(\\sqrt{x}\\frac{ \\cos(\\cos(x\\frac{\\cos(x)}{\\sin(xz)}))}{\\tan(\\sin(\\sqrt[x]{absdsd}\\frac{\\cos(x)}{\\sin(xz)}))})}@{\\cos(\\sqrt{x}\\frac{ \\cos(\\cos(x\\frac{\\cos(x)}{\\sin(xz)}))}{\\tan(\\sin(\\sqrt[x]{absdsd}\\frac{\\cos(x)}{\\sin(xz)}))})}";

//        test = "\\cos(x\\cos(x))";

        SemanticLatexParser parser = new SemanticLatexParser();
        parser.init( GlobalConstants.PATH_REFERENCE_DATA );
        parser.parse(test);
        System.out.println("Given semantic LaTeX:");
        System.out.println(test + System.lineSeparator());
        System.out.println("Converted to " + Keys.CAS_KEY + ":");
        System.out.println(parser.getTranslatedExpression());
        System.out.println();
        System.out.println(INFO_LOG.toString());
        /*
        System.out.println();
        System.out.println(parser.getExtraInformation());
        System.out.println();
        System.err.println(parser.getErrorMessage());
        */
    }
}
