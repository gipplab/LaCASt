package gov.nist.drmf.interpreter.maple.parser;

import com.maplesoft.externalcall.MapleException;
import com.maplesoft.openmaple.Algebraic;
import com.maplesoft.openmaple.Engine;
import gov.nist.drmf.interpreter.common.GlobalConstants;
import gov.nist.drmf.interpreter.common.Keys;
import gov.nist.drmf.interpreter.common.grammar.Brackets;
import gov.nist.drmf.interpreter.common.symbols.BasicFunctionsTranslator;
import gov.nist.drmf.interpreter.common.symbols.Constants;
import gov.nist.drmf.interpreter.common.symbols.GreekLetters;
import gov.nist.drmf.interpreter.common.symbols.SymbolTranslator;
import gov.nist.drmf.interpreter.maple.listener.MapleListener;
import gov.nist.drmf.interpreter.maple.parser.components.AbstractAlgebraicParser;

import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by AndreG-P on 21.02.2017.
 */
public class MapleInterface extends AbstractAlgebraicParser<Algebraic>{

    public static final Logger LOG = Logger.getLogger(MapleInterface.class.toString());

    public static final Brackets DEFAULT_LATEX_BRACKET = Brackets.left_latex_parenthesis;

    private final String[] maple_args = new String[]{"java"};

    private final String
            define_symb = ":=",
            to_inert_prefix = "ToInert('",
            to_inert_suffix = "')";

    private String maple_procedure;

    private MapleListener listener;
    private static Engine e;

    private static GreekLetters greek;
    private static Constants constants;
    private static BasicFunctionsTranslator basicFunc;
    private static SymbolTranslator symbolTranslator;

    private String translateTo;

    public MapleInterface( String translateTo ){
        this.translateTo = translateTo;

        greek = new GreekLetters(Keys.KEY_MAPLE, translateTo);
        constants = new Constants(Keys.KEY_MAPLE, translateTo);
        basicFunc = new BasicFunctionsTranslator(Keys.KEY_DLMF);
        symbolTranslator = new SymbolTranslator(Keys.KEY_MAPLE, translateTo);
    }

    /**
     * Initialize the interface to the engine of Maple. You cannot initialize it twice!
     * If the engine is already running, this function ignores other calls.
     *
     * First it is trying to load the procedure to convert the Inert-Form
     * to a list. You can find this procedure in {@link GlobalConstants#PATH_MAPLE_PROCEDURE}.
     *
     * After that, it creates an Engine object of Maple and defines the procedure once.
     *
     * @throws MapleException if the Engine cannot be initialized or the evaluation of the procedure fails.
     * @throws IOException if it cannot load the procedure from file {@link GlobalConstants#PATH_MAPLE_PROCEDURE}.
     */
    public void init() throws MapleException, IOException {
        // ignore calls if the engine already exists.
        if ( e != null ) return;

        // loading procedure from file.
        String procedure;
        // try to collect a stream.
        try ( Stream<String> stream = Files.lines( GlobalConstants.PATH_MAPLE_PROCEDURE ) ){
            procedure = stream.collect( Collectors.joining(System.lineSeparator()) );
            stream.close(); // not really necessary
            maple_procedure = procedure.split(define_symb)[0].trim();
        } catch (IOException ioe){
            System.err.println("Cannot load procedure from file " + GlobalConstants.PATH_MAPLE_PROCEDURE);
            throw ioe;
        }

        // initialize callback listener
        listener = new MapleListener(true);

        // initialize engine
        e = new Engine( maple_args, listener, null, null );

        // evaluate procedure
        e.evaluate( procedure );

        // init translators
        greek.init();
        constants.init();
        basicFunc.init();
        symbolTranslator.init();
    }

    public String parse( String maple_input ) throws MapleException {
        // Looks shitty? Right! It's Jürgen's shit...
        Algebraic a =
                e.evaluate(
                        maple_procedure + "(" +
                        to_inert_prefix + maple_input + to_inert_suffix +
                        ");"
                );


        if ( !parse(a) ){
            System.err.println("Something went wrong: " + internalErrorLog);
            return "";
        } else return translatedExpression;
    }

    @Override
    public boolean parse( Algebraic alg ){
        translatedExpression = parseGeneralExpression(alg);
        if ( translatedExpression == null ) return false;
        else return true;
    }

    public static Algebraic evaluateExpression( String exp ) throws MapleException{
        return e.evaluate( exp );
    }
}
