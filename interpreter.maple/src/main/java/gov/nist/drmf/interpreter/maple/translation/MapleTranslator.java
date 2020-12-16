package gov.nist.drmf.interpreter.maple.translation;

import com.maplesoft.externalcall.MapleException;
import com.maplesoft.openmaple.Algebraic;
import gov.nist.drmf.interpreter.common.TranslationInformation;
import gov.nist.drmf.interpreter.common.TranslationProcessConfig;
import gov.nist.drmf.interpreter.common.constants.GlobalPaths;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.exceptions.InitTranslatorException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationExceptionReason;
import gov.nist.drmf.interpreter.pom.common.grammar.Brackets;
import gov.nist.drmf.interpreter.common.interfaces.ITranslator;
import gov.nist.drmf.interpreter.common.symbols.BasicFunctionsTranslator;
import gov.nist.drmf.interpreter.common.symbols.Constants;
import gov.nist.drmf.interpreter.common.symbols.GreekLetters;
import gov.nist.drmf.interpreter.common.symbols.SymbolTranslator;
import gov.nist.drmf.interpreter.maple.common.MapleConstants;
import gov.nist.drmf.interpreter.maple.extension.MapleInterface;
import gov.nist.drmf.interpreter.maple.grammar.lexicon.MapleLexicon;
import gov.nist.drmf.interpreter.maple.translation.components.AbstractAlgebraicTranslator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The Maple backward translator. Note that there is only one interface to maple.
 * If you want to avoid overload, use the standard instance.
 *
 * @author AndreG-P on 21.02.2017.
 */
public final class MapleTranslator extends AbstractAlgebraicTranslator<Algebraic> implements ITranslator {
    /**
     * The default brackets Maple uses.
     * @see Brackets
     */
    public static final Brackets DEFAULT_LATEX_BRACKET = Brackets.left_latex_parenthesis;

    /**
     * The private Logger to log all information
     */
    private static final Logger LOG = LogManager.getLogger(MapleTranslator.class.toString());

    /**
     * Inner constants to handle maple commands
     */
    private static final String define_symb = ":=";

    /**
     * The name of the procedure to convert the inner DAG structure
     * to a list structure.
     */
    private String maple_list_procedure_name, maple_to_inert_procedure_name;

    /**
     * The basic translators.
     *  GreekLetters: To translate greek letters
     *  Constants: To translate mathematical constants
     *  BasicFunctionsTranslator: To translate functions without an extra DLMF/DRMF macro
     *  SymbolTranslator: To translate mathematical symbols
     */
    private GreekLetters greek;
    private Constants constants;
    private BasicFunctionsTranslator basicFunc;
    private SymbolTranslator symbolTranslator;

    /**
     * MapleInterface
     */
    private final MapleInterface maple;

    /**
     *
     */
    private static MapleTranslator defaultInstance;

    /**
     * It's recommended to use the default instance via
     * {@link #getDefaultInstance()} instead of creating a new object.
     * If you do, you should know what you do and why.
     */
    public MapleTranslator(){
        maple = MapleInterface.getUniqueMapleInterface();
    }

    /**
     * Get the default translator instance.
     * @return recommended way to get access to the translator instance
     */
    public static MapleTranslator getDefaultInstance() {
        if ( defaultInstance == null ) {
            try {
                defaultInstance = new MapleTranslator();
                defaultInstance.init();
            } catch (MapleException | InitTranslatorException | IOException e) {
                LOG.error("Unable to load default instance of Maple translator", e);
                defaultInstance = null;
                return null;
            }
        }
        return defaultInstance;
    }

    /**
     * Initialize the interface to the engine of Maple. You cannot initialize it twice!
     * If the engine is already running, this function ignores other calls.
     *
     * First it is trying to load the procedure to convert the Inert-Form
     * to a list. You can find this procedure in {@link GlobalPaths#PATH_MAPLE_PROCS}.
     *
     * After that, it creates an Engine object of Maple and defines the procedure once.
     *
     * @throws MapleException if the Engine cannot be initialized or the evaluation of the procedure fails.
     * @throws IOException if it cannot load the procedure from file {@link GlobalPaths#PATH_MAPLE_PROCS}.
     */
    public void init() throws MapleException, IOException, InitTranslatorException {
        LOG.debug("Start init!");

        // evaluate procedures
        loadProcedures();

        TranslationProcessConfig config = new TranslationProcessConfig(Keys.KEY_MAPLE, Keys.KEY_LATEX);
        setConfig(config);
        getConfig().init();

        greek = getConfig().getGreekLettersTranslator();
        constants = getConfig().getConstantsTranslator();
        basicFunc = getConfig().getBasicFunctionsTranslator();
        symbolTranslator = getConfig().getSymbolTranslator();

        // load the macro lexicon file
        MapleLexicon.init();

        // translate useful symbols to speed the process a bit up.
        MULTIPLY = symbolTranslator.translateFromMLPKey( Keys.MLP_KEY_MULTIPLICATION );
        ADD = symbolTranslator.translateFromMLPKey( Keys.MLP_KEY_ADDITION );
        INFINITY = constants.translate( MapleConstants.INFINITY );
    }

    private void loadProcedures() throws MapleException, IOException {
        String list_procedure = extractProcedure( GlobalPaths.PATH_MAPLE_LIST_PROCEDURE );
        this.maple_list_procedure_name = extractNameOfProcedure(list_procedure);

        String to_inert_procedure = extractProcedure( GlobalPaths.PATH_MAPLE_TO_INERT_PROCEDURE );
        this.maple_to_inert_procedure_name = extractNameOfProcedure( to_inert_procedure );

        maple.loadProcedure( list_procedure );
        maple.loadProcedure( to_inert_procedure );
    }

    public static String extractProcedure( Path maple_proc ) throws IOException {
        // try to collect a stream.
        try ( Stream<String> stream = Files.lines( maple_proc ) ){
            String procedure = stream.collect( Collectors.joining(System.lineSeparator()) );
            stream.close(); // not really necessary
            return procedure;
        } catch (IOException ioe){
            LOG.error("Cannot load procedure from file " + maple_proc);
            throw ioe;
        }
    }

    public static String extractNameOfProcedure( String maple_proc ){
        return maple_proc.split(define_symb)[0].trim();
    }

    /**
     * Translates a given maple expression. This expression should not end
     * with a semicolon, otherwise you will produce a MapleException.
     *
     * @param maple_input maple expression in 1D representation without a semicolon at the end!
     * @return the translated expression in semantic LaTeX
     * @throws MapleException if the conversion to the inner form of Maple fails.
     */
    @Override
    public TranslationInformation translateToObject( String maple_input ) throws TranslationException {
        // Creates the command by wrapping all necessary information around the input
        // to convert the given input into the internal maple datastructure
        String cmd = maple_to_inert_procedure_name + "('" + maple_input + "')";
        // to convert the internal DAG into a list representation
        cmd = maple_list_procedure_name + "(" + cmd + ");";

        try {
            // evaluates the given expression
            Algebraic a = maple.evaluate(cmd);

            // log information
            LOG.debug("Wrapping: " + cmd);
            LOG.info("Parsed: " + maple_input);
            LOG.debug("Algebraic Result: " + a.toString());

            // try to translate the expression
            // if it fails, translate will return false and the error information
            // can be accessed by the internalErrorLog

            translatedList = translateGeneralExpression(a);
            String translation = translatedList.getAccurateString();

            TranslationInformation ti = new TranslationInformation(maple_input, translation);
            ti.setInformation(getInfos());
            return ti;
        } catch (Exception e) {
            throw new TranslationException(
                    Keys.KEY_MAPLE,
                    Keys.KEY_LATEX,
                    "Cannot evaluate Maple input.",
                    TranslationExceptionReason.MLP_ERROR,
                    e
            );
        }
    }

    /**
     * Assumes an algebraic object of a Maple expression.
     * Caused by technical issues, this method converts
     * the given algebraic object into a String and evaluates
     * the String. You can use {@link #translate(String)} for get
     * the same effect.
     * @param alg algebraic object of a maple expression
     * @return true if the translation process finished without problems
     * and false if there where some problems. You get access to the
     * translated expression by {@link #getTranslatedExpression()}.
     */
    @Override
    public Boolean translate( Algebraic alg ){
        String maple_text = alg.toString();
        try {
            translate(maple_text);
            return true;
        } catch ( TranslationException me ){
            LOG.fatal("Cannot translate " + maple_text, me);
            return false;
        }
    }

    /**
     * Returns the greek letters translator.
     * @return greek letters translator
     */
    public GreekLetters getGreekTranslator(){
        return greek;
    }

    /**
     * Returns the symbol translator.
     * @return symbol translator
     */
    public SymbolTranslator getSymbolTranslator(){
        return symbolTranslator;
    }

    /**
     * Returns the constants translator
     * @return constants translator
     */
    public Constants getConstantsTranslator(){
        return constants;
    }

    /**
     * Returns the basic functions translator object.
     * @return basic functions translator
     */
    public BasicFunctionsTranslator getBasicFunctionsTranslator(){
        return basicFunc;
    }
}
