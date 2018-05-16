package gov.nist.drmf.interpreter.maple.translation;

import com.maplesoft.externalcall.MapleException;
import com.maplesoft.openmaple.Algebraic;
import com.maplesoft.openmaple.Engine;
import gov.nist.drmf.interpreter.common.GlobalPaths;
import gov.nist.drmf.interpreter.common.Keys;
import gov.nist.drmf.interpreter.common.grammar.Brackets;
import gov.nist.drmf.interpreter.common.symbols.BasicFunctionsTranslator;
import gov.nist.drmf.interpreter.common.symbols.Constants;
import gov.nist.drmf.interpreter.common.symbols.GreekLetters;
import gov.nist.drmf.interpreter.common.symbols.SymbolTranslator;
import gov.nist.drmf.interpreter.maple.common.MapleConstants;
import gov.nist.drmf.interpreter.maple.grammar.lexicon.MapleLexicon;
import gov.nist.drmf.interpreter.maple.listener.MapleListener;
import gov.nist.drmf.interpreter.maple.setup.Initializer;
import gov.nist.drmf.interpreter.maple.translation.components.AbstractAlgebraicTranslator;
import gov.nist.drmf.interpreter.mlp.extensions.MacrosLexicon;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Observer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The interface to Maple.
 *
 * There is only one instance of this class allowed during the runtime. To get
 * access to this instance, you have to initialize it first and invoke {@link #init()}
 * and the instance with {@link #getUniqueMapleInterface()} after the initialization
 * process has finished.
 *
 * Created by AndreG-P on 21.02.2017.
 */
public final class MapleInterface extends AbstractAlgebraicTranslator<Algebraic> {
    /**
     * The default brackets Maple uses.
     * @see Brackets
     */
    public static final Brackets DEFAULT_LATEX_BRACKET = Brackets.left_latex_parenthesis;

    /**
     * The private Logger to log all information
     */
    private static final Logger LOG = LogManager.getLogger(MapleInterface.class.toString());

    /**
     * Inner constant to initialize Maple
     */
    private final String[] maple_args = new String[]{"java"};

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
     * The engine is the openmaple Interface to interact with Maple
     */
    private Engine engine;

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
     * Unique listener of the process
     */
    private static MapleListener listener;

    /**
     * There is only one instance at runtime allowed. You get access
     * to this instance via {@link #init()} and {@link #getUniqueMapleInterface()}.
     */
    private MapleInterface(){}

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
    private void inner_init() throws MapleException, IOException {
        LOG.info("Start init! Load maple native libraries!");
        if ( Initializer.loadMapleNatives() )
            LOG.info("Loaded Maple Natives!");
        else {
            LOG.error("Cannot load maple native directory.");
            return;
        }

        // initialize callback listener
        listener = new MapleListener(true);

        // initialize engine
        engine = new Engine( maple_args, listener, null, null );

        // evaluate procedures
        loadProcedures();

        // set up all translators, define the direction of translation
        greek = new GreekLetters( Keys.KEY_MAPLE, Keys.KEY_LATEX );
        constants = new Constants( Keys.KEY_MAPLE, Keys.KEY_DLMF );
        basicFunc = new BasicFunctionsTranslator( Keys.KEY_LATEX );
        symbolTranslator = new SymbolTranslator( Keys.KEY_MAPLE, Keys.KEY_LATEX );
        // init all translators
        greek.init();
        constants.init();
        basicFunc.init();
        symbolTranslator.init();

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

        engine.evaluate( list_procedure );
        engine.evaluate( to_inert_procedure );
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
    public String translate( String maple_input ) throws MapleException {
        // Creates the command by wrapping all necessary information around the input
        // to convert the given input into the internal maple datastructure
        String cmd = maple_to_inert_procedure_name + "('" + maple_input + "')";
        // to convert the internal DAG into a list representation
        cmd = maple_list_procedure_name + "(" + cmd + ");";

        // evaluates the given expression
        Algebraic a = engine.evaluate(cmd);
        // log information
        LOG.debug("Wrapping: " + cmd);
        LOG.info("Parsed: " + maple_input);
        LOG.debug("Algebraic Result: " + a.toString());

        // try to translate the expression
        // if it fails, translate will return false and the error information
        // can be accessed by the internalErrorLog
        try {
            translatedList = translateGeneralExpression(a);
            return translatedList.getAccurateString();
        } catch ( Exception e ){
            LOG.error( "Cannot translate given maple input.", e );
            return "";
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
    public boolean translate( Algebraic alg ){
        String maple_text = alg.toString();
        try {
            translate(maple_text);
            return true;
        } catch ( MapleException me ){
            LOG.fatal("Cannot translate " + maple_text, me);
            return false;
        }
    }

    /**
     * It evaluates the given expression via Maple.
     * Make sure your expression ends with a semicolon.
     * Otherwise you will produce a MapleException.
     * @param exp syntactical correct Maple expression ended with a semicolon
     * @return the algebraic object of the result.
     * @throws MapleException if Maple produces an error
     */
    public Algebraic evaluateExpression( String exp ) throws MapleException {
        return engine.evaluate( exp );
    }

    /**
     *
     * @return
     * @throws MapleException
     */
    public void invokeGC() throws MapleException {
        LOG.debug("Manually invoke Maple's garbage collector.");
        engine.evaluate("gc();");
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

    // The unique maple interface object
    private static MapleInterface mInterface;

    /**
     * Instantiate the interface to Maple. If it is already
     * instantiate, nothing will happen. You can get the instance
     * by invoke {@link #getUniqueMapleInterface()}.
     *
     * The initialization process can be split into four parts.
     *  1)  It tries to load the Maple native libraries into the
     *      java.library.path with the {@link Initializer} class.
     *      This is necessary to connect this java program with
     *      Maple. You can define the path in libs/maple_config.properties.
     *      This stage produces no exceptions but is necessary to
     *      finish the initialization process of this program.
     *      It stops if it is not possible to load the native libraries.
     *
     *  2)  It loads the Maple procedure from libs/ReferenceData/MapleProcedures
     *      to convert the inert-form of a Maple expression to a
     *      Maple list of the inert-form. This could produces an {@link IOException}
     *      if it is not possible to load the procedure from the file.
     *
     *  3)  It starts the Maple engine. This could produces a {@link MapleException}
     *      if the initialization of Maple's engine fails or the evaluation
     *      of the loaded procedure fails.
     *
     *  4)  It loads the necessary translation files to translate greek letters,
     *      mathematical constants and functions. Since it loads those translations
     *      from files, this part can produces an {@link IOException} again.
     *
     * @throws MapleException   if it is not possible to initiate the {@link Engine}
     *                          from the openmaple API or the evaluation of the
     *                          pre-defined Maple procedure fails.
     * @throws IOException  if it is not possible to read the translation information
     *                      from the files in libs or the Maple procedure in the file
     *                      maple_list_procedure.txt.
     */
    public static void init() throws MapleException, IOException {
        if ( mInterface != null ) {
            //LOG.debug("Maple interface already instantiated!");
            return;
        }
        mInterface = new MapleInterface();
        mInterface.inner_init();
    }

    /**
     * Restarts the Maple session and reloads the Maple procedures.
     * Note that you have to reload your own settings again after a
     * restart of the engine.
     *
     * @throws MapleException if the engine cannot be restarted
     * @throws IOException if the procedures cannot be loaded
     */
    public void restart() throws MapleException, IOException {
        engine.restart();
        loadProcedures();
    }

    public void addMemoryObserver(Observer observer){
        listener.addObserver(observer);
    }

    /**
     * Returns the unique interface to Maple. This might be
     * null, if you haven't invoke {@link #init()} previously!
     *
     * @return the unique object of the interface to Maple. Can
     * be null.
     */
    public static MapleInterface getUniqueMapleInterface(){
        return mInterface;
    }

    /**
     * Return the unique listener of current Maple process.
     *
     * @return unique listener
     */
    public static MapleListener getUniqueMapleListener(){ return listener; }
}
