package gov.nist.drmf.interpreter.cas;

import gov.nist.drmf.interpreter.cas.logging.InformationLogger;
import gov.nist.drmf.interpreter.cas.parser.SemanticLatexParser;
import gov.nist.drmf.interpreter.common.GlobalConstants;
import gov.nist.drmf.interpreter.common.Keys;
import gov.nist.drmf.interpreter.common.symbols.BasicFunctionsTranslator;
import gov.nist.drmf.interpreter.common.symbols.Constants;
import gov.nist.drmf.interpreter.common.symbols.GreekLetters;

import java.io.Console;
import java.util.Arrays;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The main class to translate semantic LaTeX
 * to a given CAS.
 *
 * @author Andre Greiner-Petter
 */
public class SemanticToCASInterpreter {
    public static final String NEW_LINE = System.lineSeparator();

    public static final Logger LOG = Logger.getLogger( SemanticToCASInterpreter.class.toString() );

    private static long init_ms, trans_ms;

    public static void main(String[] args){
        Scanner console = new Scanner(System.in);
        if ( console == null ){
            System.err.println("Cannot start the program! The system console is missing.");
            return;
        }

        if ( args != null && args.length >= 1 && args[0].matches("(-+h)|(-*help)") ){
            String help = "When you start this program without any flags" +
                    NEW_LINE +
                    "it will ask you all necessary information." + NEW_LINE +
                    "But you are able to set flags at program start if you want.";
            help += NEW_LINE;
            help += "   -CAS=<NameOfCAS>   " + " " + "<- Sets the CAS." + NEW_LINE;
            help += "   -Expression=\"<Exp>\"" + " " + "<- Sets the expression you " +
                    "want to translate. (Make sure you use \"..\")" + NEW_LINE;
            help += "   -extra" + "              " + "<- Shows extra information about the translation." + NEW_LINE;
            help += "   -debug" + "              " + "<- Sets the debug flag for a bit more detailed output.";
            System.out.println(help);
            return;
        }

        String hello = NEW_LINE +
                "This is a program that translated given LaTeX" + NEW_LINE +
                "code into a specified computer algebra system" + NEW_LINE +
                "representation." + NEW_LINE ;

        System.out.println( hello );
        String CAS = null;
        String expression = null;
        boolean debug = false;
        boolean extra = false;

        if ( args != null ){
            for ( int i = 0; i < args.length; i++ ){
                String flag = args[i];
                if ( flag.matches( "-CAS=.+" ) ){
                    CAS = flag.substring(5);
                } else if ( flag.matches( "-Expression=.+" ) ){
                    expression = flag.substring( "-Expression=".length() );
                } else if ( flag.matches( "-debug" ) )
                    debug = true;
                else if ( flag.matches( "-extra" ) )
                    extra = true;
            }
        }

        if ( CAS == null ){
            System.out.println( "To which CAS you want to translate your expression:" );
            CAS = console.nextLine();
            System.out.println();
        } else {
            System.out.println("You set the following CAS: " + CAS + NEW_LINE);
        }

        if ( CAS == null || CAS.isEmpty() ){
            System.err.println("You didn't specified a CAS. Please start the program again to try it once more.");
            return;
        }

        if ( expression != null ){
            System.out.println("You want to translate the following expression: " + expression + NEW_LINE);
        } else {
            System.out.println("Which expression do you want to translate:");
            expression = console.nextLine();
            System.out.println();
        }

        if ( expression == null || expression.isEmpty() ){
            System.err.println("You didn't give an expression to translate.");
            return;
        }

        System.out.println("Set global variable to given CAS.");
        init_ms = System.currentTimeMillis();
        Keys.CAS_KEY = CAS;

        System.out.println("Set up parser...");
        SemanticLatexParser latexParser =
                new SemanticLatexParser( Keys.KEY_LATEX, Keys.CAS_KEY );

        System.out.println("Initialize parser...");
        latexParser.init( GlobalConstants.PATH_REFERENCE_DATA );
        init_ms = System.currentTimeMillis()-init_ms;

        System.out.println("Start translation...");
        System.out.println();
        trans_ms = System.currentTimeMillis();
        latexParser.parse( expression );
        trans_ms = System.currentTimeMillis()-trans_ms;

        System.out.println("Finished conversion to " + Keys.CAS_KEY + ":");
        System.out.println(latexParser.getTranslatedExpression());
        System.out.println();

        if ( debug ){
            System.out.println( "DEBUGGING Components: " + NEW_LINE +
                    Arrays.toString(latexParser.getGlobalExpressionObject().trans_exps.toArray()));
            System.out.println();
            System.out.println("Initialization takes: " + init_ms + "ms");
            System.out.println("Translation process takes: " + trans_ms + "ms");
            System.out.println();
        }

        if ( extra ){
            System.out.println(latexParser.getInfoLog().toString());
        }

        /*
        Keys.CAS_KEY = Keys.KEY_MAPLE;

        String test = "";
        if ( args == null ) {
            LOG.severe("Need a given expression. Try \\JacobiP{a}{b}{c}@{d} for instance.");
            return;
        }

        for ( int i = 0; i < args.length; i++ )
            test += args[i];

//        test = "\\cos\\frac{1}{2}2";
//        test = "(ab^2c13b+2) \\cdot \\CatalansConstant 2";
//        test = "\\JacobiP{(a! \\mod b^2)!!}{0}{0}@{0}";
//        test = "\\cos \\left( 1^{2^{3+3}*\\iunit} \\right)";
//        test = "\\cos@{2*\\iunit!}!^2 \\mod 2";
//        test = "x^{\\JacobiP{\\iunit}{b}{c}@{d}}!";
//        test = "\\sqrt[\\alpha]{\\cpi}+2\\JacobiP{i}{\\beta}{2}@{12.6}!";
//        test = "q*\\iunit+\\cos(2-\\frac{\\sqrt[\\alpha]{\\cpi}}{2\\JacobiP{i}{\\beta}{2}@{12.6}})";
//        test = "18*\\JacobiP{\\cos{\\sqrt{i}}}{\\frac{1}{\\cpi}}{2.0}@{\\gamma}";
//        test = "\\JacobiP{\\alpha}{b}{c}@{\\cos(\\sqrt{x}\\frac{ \\cos(\\cos(x\\frac{\\cos(x)}{\\sin(xz)}))}{\\tan(\\sin(\\sqrt[x]{absdsd}\\frac{\\cos(x)}{\\sin(xz)}))})}";
//        test = "\\JacobiP{\\alpha\\sqrt[3]{x}\\sin(x\\alpha xyz)\\sqrt[2]{3}}{b\\frac{1}{\\pi}}{1+0\\cos(\\sqrt{x}\\frac{ \\cos(\\cos(x\\frac{\\cos(x)}{\\sin(xz)}))}{\\tan(\\sin(\\sqrt[x]{absdsd}\\frac{\\cos(x)}{\\sin(xz)}))})}@{\\cos(\\sqrt{x}\\frac{ \\cos(\\cos(x\\frac{\\cos(x)}{\\sin(xz)}))}{\\tan(\\sin(\\sqrt[x]{absdsd}\\frac{\\cos(x)}{\\sin(xz)}))})}";


        latexParser.init( GlobalConstants.PATH_REFERENCE_DATA );
        latexParser.parse(test);
        */
    }
}
