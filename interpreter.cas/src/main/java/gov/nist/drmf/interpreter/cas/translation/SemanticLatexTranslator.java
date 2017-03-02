package gov.nist.drmf.interpreter.cas.translation;

import gov.nist.drmf.interpreter.cas.logging.InformationLogger;
import gov.nist.drmf.interpreter.cas.logging.TranslatedExpression;
import gov.nist.drmf.interpreter.common.GlobalConstants;
import gov.nist.drmf.interpreter.common.GlobalPaths;
import gov.nist.drmf.interpreter.common.Keys;
import gov.nist.drmf.interpreter.common.symbols.BasicFunctionsTranslator;
import gov.nist.drmf.interpreter.common.symbols.Constants;
import gov.nist.drmf.interpreter.common.symbols.GreekLetters;
import gov.nist.drmf.interpreter.common.symbols.SymbolTranslator;
import mlp.ParseException;
import mlp.PomParser;
import mlp.PomTaggedExpression;

import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This translation translate semantic LaTeX formula using
 * the math processor language by Abdou Youssef.
 * It based on BNF grammar programmed with JavaCC.
 *
 * It is the top level translation objects. That means
 * you can use {@link #parse(String)} to translate an
 * expression in general. To do so, you have to
 * invoke {@link #init(Path)} before you use this
 * translate method. On the other hand this translation can
 * handle also general PomTaggedExpression to translate.
 * @see PomTaggedExpression
 *
 * @author Andre Greiner-Petter
 */
public class SemanticLatexTranslator extends AbstractTranslator {
    public static String TAB = "";

    private static GreekLetters greekLetters;
    private static Constants constants;
    private static BasicFunctionsTranslator functions;
    private static SymbolTranslator symbols;

    private PomParser parser;

    public SemanticLatexTranslator(String from_language, String to_language ){
        greekLetters = new GreekLetters(from_language, to_language);
        constants = new Constants(Keys.KEY_DLMF, to_language);
        functions = new BasicFunctionsTranslator(to_language);
        symbols = new SymbolTranslator(from_language, to_language);

        INFO_LOG = new InformationLogger();
        ERROR_LOG = Logger.getLogger( SemanticLatexTranslator.class.toString() );
        ERROR_LOG.setLevel(Level.WARNING);

        global_exp = new TranslatedExpression();
        int length = GlobalConstants.CAS_KEY.length()+1 > "DLMF: ".length() ?
                (GlobalConstants.CAS_KEY.length()+2) : "DLMF: ".length();
        for ( int i = 0; i <= length; i++ )
            TAB += " ";
    }

    /**
     * Initialize translation.
     * @param reference_dir_path
     */
    public void init( Path reference_dir_path ){
        greekLetters.init();
        constants.init();
        functions.init();
        symbols.init();

        MULTIPLY = symbols.translateFromMLPKey( Keys.MLP_KEY_MULTIPLICATION );

        parser = new PomParser(reference_dir_path.toString());
        parser.addLexicons( GlobalPaths.DLMF_MACROS_LEXICON_NAME );
    }

    /**
     *
     * @param expression
     * @return
     */
    public boolean parse(String expression){
        try {
            PomTaggedExpression exp = parser.parse(expression);
            return translate(exp);
        } catch ( ParseException pe ){
            return false;
        }
    }

    @Override
    public boolean translate(PomTaggedExpression expression) {
        reset();
        local_inner_exp.addTranslatedExpression(
                parseGeneralExpression(expression, null).getTranslatedExpression()
        );
        return !isInnerError();
    }

    public static GreekLetters getGreekLettersParser(){
        return greekLetters;
    }

    public static Constants getConstantsParser(){
        return constants;
    }

    public static BasicFunctionsTranslator getBasicFunctionParser(){
        return functions;
    }

    public static SymbolTranslator getSymbolsTranslator(){
        return symbols;
    }

    public InformationLogger getInfoLog(){
        return INFO_LOG;
    }
}
