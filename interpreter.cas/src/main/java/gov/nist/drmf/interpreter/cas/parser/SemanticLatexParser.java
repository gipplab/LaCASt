package gov.nist.drmf.interpreter.cas.parser;

import gov.nist.drmf.interpreter.cas.logging.InformationLogger;
import gov.nist.drmf.interpreter.cas.logging.TranslatedExpression;
import gov.nist.drmf.interpreter.common.GlobalConstants;
import gov.nist.drmf.interpreter.common.Keys;
import gov.nist.drmf.interpreter.common.symbols.BasicFunctionsTranslator;
import gov.nist.drmf.interpreter.common.symbols.Constants;
import gov.nist.drmf.interpreter.common.symbols.GreekLetters;
import mlp.ParseException;
import mlp.PomParser;
import mlp.PomTaggedExpression;

import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This parser parse semantic LaTeX formula using
 * the math processor language by Abdou Youssef.
 * It based on BNF grammar programmed with JavaCC.
 *
 * It is the top level parser objects. That means
 * you can use {@link #parse(String)} to parse an
 * expression in general. To do so, you have to
 * invoke {@link #init(Path)} before you use this
 * parse method. On the other hand this parser can
 * handle also general PomTaggedExpression to parse.
 * @see PomTaggedExpression
 *
 * @author Andre Greiner-Petter
 */
public class SemanticLatexParser extends AbstractParser {
    private static GreekLetters greekLetters;
    private static Constants constants;
    private static BasicFunctionsTranslator functions;

    private PomParser parser;

    public SemanticLatexParser( String from_language, String to_language ){
        greekLetters = new GreekLetters(from_language, to_language);
        constants = new Constants(Keys.KEY_DLMF, to_language);
        functions = new BasicFunctionsTranslator(to_language);

        INFO_LOG = new InformationLogger();
        ERROR_LOG = Logger.getLogger( SemanticLatexParser.class.toString() );
        ERROR_LOG.setLevel(Level.WARNING);

        global_exp = new TranslatedExpression();
    }

    /**
     * Initialize parser.
     * @param reference_dir_path
     */
    public void init( Path reference_dir_path ){
        greekLetters.init();
        constants.init();
        functions.init();
        parser = new PomParser(reference_dir_path.toString());
        parser.addLexicons( GlobalConstants.DLMF_MACROS_LEXICON_NAME );
    }

    /**
     *
     * @param expression
     * @return
     */
    public boolean parse(String expression){
        try {
            PomTaggedExpression exp = parser.parse(expression);
            return parse(exp);
        } catch ( ParseException pe ){
            return false;
        }
    }

    @Override
    public boolean parse(PomTaggedExpression expression) {
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

    public InformationLogger getInfoLog(){
        return INFO_LOG;
    }
}
