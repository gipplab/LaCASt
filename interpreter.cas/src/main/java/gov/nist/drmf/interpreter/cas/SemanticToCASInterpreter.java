package gov.nist.drmf.interpreter.cas;

import gov.nist.drmf.interpreter.cas.logging.InformationLogger;
import gov.nist.drmf.interpreter.cas.parser.SemanticLatexParser;
import gov.nist.drmf.interpreter.common.GlobalConstants;
import gov.nist.drmf.interpreter.common.Keys;
import gov.nist.drmf.interpreter.common.symbols.BasicFunctionsTranslator;
import gov.nist.drmf.interpreter.common.symbols.Constants;
import gov.nist.drmf.interpreter.common.symbols.GreekLetters;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The main class to translate semantic LaTeX
 * to a given CAS.
 *
 * @author Andre Greiner-Petter
 */
public class SemanticToCASInterpreter {
    public static final Logger LOG = Logger.getLogger( SemanticToCASInterpreter.class.toString() );

    public static void main(String[] args){
        Keys.CAS_KEY = Keys.KEY_MAPLE;

        SemanticLatexParser latexParser =
                new SemanticLatexParser( Keys.KEY_LATEX, Keys.CAS_KEY );

        String test = "";
        if ( args == null ) {
            LOG.severe("Need a given expression. Try \\JacobiP{a}{b}{c}@{d} for instance.");
            return;
        }
        for ( int i = 0; i < args.length; i++ )
            test += args[i];

//        test = "\\cos \\left( 2+2 \\right)";
//        test = "\\frac{1+1}{2+2}+2";
//        test = "(abc1+2) \\cdot \\CatalansConstant";
//        test = "\\sqrt[\\alpha]{\\cpi}+2\\JacobiP{i}{\\beta}{2}@{12.6}";
//        test = "q*\\iunit+\\cos(2-\\frac{\\sqrt[\\alpha]{\\cpi}}{2\\JacobiP{i}{\\beta}{2}@{12.6}})";
//        test = "18*\\JacobiP{\\cos{\\sqrt{2}}}{\\JacobiP{\\iunit}{b}{c}@{d}}{2}@{3}";
//        test = "x^{\\JacobiP{\\iunit}{b}{c}@{d}}";
//        test = "\\JacobiP{\\alpha}{b}{c}@{\\cos(\\sqrt{x}\\frac{ \\cos(\\cos(x\\frac{\\cos(x)}{\\sin(xz)}))}{\\tan(\\sin(\\sqrt[x]{absdsd}\\frac{\\cos(x)}{\\sin(xz)}))})}";
//        test = "\\JacobiP{\\alpha\\sqrt[3]{x}\\sin(x\\alpha xyz)\\sqrt[2]{3}}{b\\frac{1}{\\pi}}{1+0\\cos(\\sqrt{x}\\frac{ \\cos(\\cos(x\\frac{\\cos(x)}{\\sin(xz)}))}{\\tan(\\sin(\\sqrt[x]{absdsd}\\frac{\\cos(x)}{\\sin(xz)}))})}@{\\cos(\\sqrt{x}\\frac{ \\cos(\\cos(x\\frac{\\cos(x)}{\\sin(xz)}))}{\\tan(\\sin(\\sqrt[x]{absdsd}\\frac{\\cos(x)}{\\sin(xz)}))})}";


        latexParser.init( GlobalConstants.PATH_REFERENCE_DATA );
        latexParser.parse(test);
        System.out.println("Given semantic LaTeX:");
        System.out.println(test + System.lineSeparator());
        System.out.println("Converted to " + Keys.CAS_KEY + ":");
        System.out.println(latexParser.getTranslatedExpression());
        System.out.println( "DEBUGGING Components Information: " + System.lineSeparator() +
                Arrays.toString(latexParser.getGlobalExpressionObject().trans_exps.toArray()));
        System.out.println();
        System.out.println(latexParser.getInfoLog().toString());
    }
}
