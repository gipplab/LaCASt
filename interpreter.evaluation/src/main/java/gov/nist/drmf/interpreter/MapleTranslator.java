package gov.nist.drmf.interpreter;

import com.maplesoft.externalcall.MapleException;
import com.maplesoft.openmaple.Algebraic;
import gov.nist.drmf.interpreter.cas.translation.SemanticLatexTranslator;
import gov.nist.drmf.interpreter.common.constants.GlobalConstants;
import gov.nist.drmf.interpreter.common.constants.GlobalPaths;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.grammar.IComputerAlgebraSystemEngine;
import gov.nist.drmf.interpreter.common.grammar.ITranslator;
import gov.nist.drmf.interpreter.constraints.IConstraintTranslator;
import gov.nist.drmf.interpreter.maple.translation.MapleInterface;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Observer;
import java.util.Properties;

/**
 * This class can be used as an interface with the translation
 * programs between semantic LaTeX and Maple. It provides some
 * typical functions to translate expressions.
 *
 * Be aware you have to initialize the program before you run
 * any translations with the {@link #init()} function.
 *
 * To run correctly, it is also necessary to set the path to the
 * native libraries of your installed version of Maple. This path
 * is defined in the libs folder in the maple_config.properties
 * file. You can change the path directly in the file or invoke
 * the {@link #MapleTranslator(Path)} constructor. This will change the
 * setting in the file automatically.
 *
 * Created by AndreG-P on 03.03.2017.
 * @see Translation
 * @see Algebraic
 * @see MapleException
 * @see TranslationException
 * @see com.maplesoft.openmaple.Engine
 * @see gov.nist.drmf.interpreter.maple.translation.MapleInterface
 * @see gov.nist.drmf.interpreter.cas.translation.SemanticLatexTranslator
 */
@SuppressWarnings("ALL")
public class MapleTranslator implements IConstraintTranslator, IComputerAlgebraSystemEngine<Algebraic> {
    /**
     * The logger.
     */
    private static final Logger LOG = LogManager.getLogger( MapleTranslator.class );

    /**
     * The interface to interact with the Maple translator
     */
    private static MapleInterface mapleInterface;

    /**
     * The maple simplifier
     */
    private MapleSimplifier simplifier;

    /**
     * The interface to interact with the DLMF LaTeX translator
     */
    private SemanticLatexTranslator dlmfInterface;

    /**
     * A simple version of a Maple -> semantic LaTeX -> Maple round trip
     * translator. You can specify a translation in the first argument. If
     * not the program asks you about it.
     * @param args empty or a maple expression in the first argument
     */
    public static void main(String[] args) throws IOException, MapleException {
        // TODO

        String in = "n\\idot\\Sum{k}{1}{n}@{\\frac{1}{y^{k}}}";
        MapleTranslator t = new MapleTranslator();
        t.init();
        String maple = t.translateFromLaTeXToMapleClean(in, null);
        System.out.println(maple);
    }

    /**
     * Creates an object of the translator class.
     * It is highly recommended to just instantiate one MapleTranslator
     * class at all!
     *
     * Before you start any translation process, you have to invoke
     * the {@link #init()} method once!
     */
    public MapleTranslator(){}

    /**
     * Creates an object of the translator class with a specified
     * path to your maple native libraries. You can change this path
     * manually in libs/maple_config.properties.
     * This constructor changes the maple_config.properties file and
     * invoke the default constructor.
     *
     * It is highly recommended to just instantiate one MapleTranslator
     * class at all!
     *
     * Before you start any translation process, you have to invoke
     * the {@link #init()} method once!
     *
     * @param maple_dir the path to your maple native libraries. You
     *                  can enter the maple command "kernelopts(mapledir);"
     *                  in Maple to figure out the correct path.
     *                  You can enter this path manually to the
     *                  libs/maple_config.properties file.
     */
    public MapleTranslator(Path maple_dir ){
        this();
        try (FileOutputStream out =
                     new FileOutputStream( GlobalPaths.PATH_MAPLE_CONFIG.toFile() )){
            Properties props = new Properties();
            props.setProperty( Keys.KEY_MAPLE_BIN, maple_dir.toAbsolutePath().toString() );
            props.store(out, GlobalConstants.PROPS_COMMENTS);
            LOG.debug("Finished to setup the properties file.");
        } catch ( IOException ioe ){
            LOG.fatal( "Cannot write the path into the properties file.", ioe );
        }
    }

    /**
     * Initialize both back ends. The Maple interface will be initialized
     * first. Take a look at {@link MapleInterface#init()} and
     * {@link SemanticLatexTranslator#init(Path)} for more details
     * about the initialization process.
     *
     * @throws MapleException appears when the initialization of the
     *                      {@link com.maplesoft.openmaple.Engine} fails or
     *                      Maple is not able to evaluate the previously
     *                      loaded procedure.
     * @throws IOException  If it is not possible to load translation
     *                      information from files.
     */
    public void init() throws MapleException, IOException {
        // setup logging
        System.setProperty( Keys.KEY_SYSTEM_LOGGING, GlobalPaths.PATH_LOGGING_CONFIG.toString() );

        MapleInterface.init();
        mapleInterface = MapleInterface.getUniqueMapleInterface();
        LOG.debug("Initialized Maple Interface.");

        dlmfInterface = new SemanticLatexTranslator( Keys.KEY_MAPLE );
        dlmfInterface.init( GlobalPaths.PATH_REFERENCE_DATA );
        LOG.debug("Initialized DLMF LaTeX Interface.");

        simplifier = new MapleSimplifier( mapleInterface );
    }

    @Override
    public Algebraic enterCommand(String command) throws ComputerAlgebraSystemEngineException {
        try {
            return enterMapleCommand(command);
        } catch ( MapleException me ) {
            throw new ComputerAlgebraSystemEngineException(me);
        }
    }

    @Override
    public String translate(String expression, String label) throws TranslationException {
        return translateFromLaTeXToMapleClean(expression, label);
    }

    /**
     * Restarts the maple session. This will clear the internal memory.
     * In consequence, you have to reload all custom scripts and set all
     * variables again. Try to avoid restarting the engine again and again!
     *
     * @throws MapleException if the restart throughs an error
     * @throws IOException if the default scripts cannot be loaded
     */
    public void restartMapleSession() throws MapleException, IOException {
        mapleInterface.restart();
    }

    public void addMapleMemoryObserver( Observer observer ){
        mapleInterface.addMemoryObserver( observer );
    }

    /**
     * Returns the maple simplifier to simplify expressions.
     * This method returns null, if you invoke it before
     * {@link #init()}!
     *
     * @return the maple implifier or null, if you havn't invoked {@link #init()}
     * @see MapleSimplifier
     * @see #init()
     */
    public MapleSimplifier getMapleSimplifier(){
        return simplifier;
    }

    /**
     * Translates a given semantic LaTeX expression into an equivalent Maple string.
     * @param latex_expression expression in semantic LaTeX
     * @param label label of the latex expression (can be null if there is none)
     * @return equivalent expression in Maple syntax
     * @throws TranslationException if the translation fails.
     */
    public String translateFromLaTeXToMapleClean( String latex_expression, String label )
            throws TranslationException{
        return translateFromLaTeXToMaple( latex_expression, label ).getTranslatedExpression();
    }

    /**
     * Translates a given semantic LaTeX expression in Set mode into an equivalent Maple string.
     * @param latex_expression expression in semantic LaTeX
     * @param label label of the latex expression (can be null if there is none)
     * @return equivalent expression in Maple syntax
     * @throws TranslationException if the translation fails.
     */
    public String translateFromLaTeXToMapleSetModeClean( String latex_expression, String label )
            throws TranslationException{
        dlmfInterface.activateSetMode();
        String translation = translateFromLaTeXToMaple( latex_expression, label ).getTranslatedExpression();
        dlmfInterface.deactivateSetMode();
        return translation;
    }

    /**
     * Translates a given semantic LaTeX expression into an equivalent Maple expression.
     * The {@link Translation} is a java bean to save the translated expression and
     * further information about the translation. This object doesn't contains an
     * {@link Algebraic} object of the translation. Use
     * {@link #translateFromLaTeXToMapleAlgebraicClean(String)}
     * or {@link #translateFromLaTeXToMapleAlgebraic(String)} to get this object.
     *
     * @param latex_expression expression in semantic LaTeX
     * @param label label of the latex expression (can be null if there is none)
     * @return equivalent expression in Maple syntax in a {@link Translation} object.
     * @throws TranslationException if the translation fails.
     */
    public Translation translateFromLaTeXToMaple( String latex_expression, String label )
            throws TranslationException {
        String translation = dlmfInterface.translate( latex_expression, label );
        return new Translation(
                translation,
                dlmfInterface.getInfoLogger().toString() );
    }

    /**
     * Translates a given semantic LaTeX expression into an equivalent Maple expression
     * and returns the {@link Algebraic} object of the translated expression.
     *
     * @param latex_expression expression in semantic LaTeX
     * @param label label of the latex expression (can be null if there is none)
     * @return equivalent expression in Maple syntax as an {@link Algebraic} object.
     * @throws TranslationException if the translation fails.
     * @throws MapleException if the conversion into an {@link Algebraic} object fails.
     */
    public Algebraic translateFromLaTeXToMapleAlgebraicClean( String latex_expression, String label )
            throws TranslationException, MapleException{
        return translateFromLaTeXToMapleAlgebraic( latex_expression, label ).getAlgebraicTranslatedExpression();
    }

    /**
     * Translates a given semantic LaTeX expression into an equivalent Maple expression
     * and returns the {@link Translation} object of the translated expression. This
     * object contains an {@link Algebraic} object of the translated expression also!
     *
     * @param latex_expression expression in semantic LaTeX
     * @param label label of the latex expression (can be null if there is none)
     * @return equivalent expression in Maple syntax as an {@link Translation} object.
     *          This object also contains the {@link Algebraic} object of the translated
     *          expression.
     * @throws TranslationException if the translation fails.
     * @throws MapleException if the conversion into an {@link Algebraic} object fails.
     */
    public Translation translateFromLaTeXToMapleAlgebraic( String latex_expression, String label )
            throws TranslationException, MapleException {
        Translation t = translateFromLaTeXToMaple( latex_expression, label );
        Algebraic a = mapleInterface.evaluateExpression( "'" + t.getTranslatedExpression() + "'" );
        return new Translation( a, t.getTranslatedExpression(), t.getAdditionalInformation() );
    }

    /**
     * Translates a given Maple expression into the equivalent semantic LaTeX expression and
     * returns the string of this translated expression.
     *
     * @param maple_expression expression in Maple syntax
     * @return the equivalent semantic LaTeX expression.
     * @throws TranslationException if the translation failed
     * @throws MapleException if the evaluation of the given string failed
     */
    public String translateFromMapleToLaTeXClean( String maple_expression )
            throws TranslationException, MapleException {
        return translateFromMapleToLaTeX( maple_expression ).getTranslatedExpression();
    }

    /**
     * Translates a given Maple expression into the equivalent semantic LaTeX expression and
     * returns the {@link Translation} object of this translated expression.
     *
     * @param maple_expression expression in Maple syntax
     * @return  the equivalent semantic LaTeX expression and further
     *          information about the translation
     * @throws TranslationException if the translation failed
     * @throws MapleException if the evaluation of the given string failed
     */
    public Translation translateFromMapleToLaTeX( String maple_expression )
            throws TranslationException, MapleException {
        String trans = mapleInterface.translate( maple_expression );
        return new Translation( trans, mapleInterface.getInfos().toString() );
    }

    /**
     * Translates a given Maple {@link Algebraic} object into the equivalent
     * semantic LaTeX expression and returns the string of this translated
     * expression.
     *
     * @param maple_expression algebraic object of a Maple expression
     * @return the equivalent semantic LaTeX expression
     * @throws TranslationException if the translation failed
     * @throws MapleException if the evaluation of the given algebraic object failed
     */
    public String translateFromMapleToLaTeXClean( Algebraic maple_expression )
            throws TranslationException, MapleException {
        return translateFromMapleToLaTeX( maple_expression ).getTranslatedExpression();
    }

    /**
     * Translates a given Maple {@link Algebraic} object into the equivalent
     * semantic LaTeX expression and returns the {@link Translation} object
     * of this translated expression.
     *
     * @param maple_expression algebraic object of a Maple expression
     * @return  the equivalent semantic LaTeX expression and further
     *          information about the translation.
     * @throws TranslationException if the translation failed
     * @throws MapleException if the evaluation of the given algebraic object failed
     */
    public Translation translateFromMapleToLaTeX( Algebraic maple_expression )
            throws TranslationException, MapleException {
        String maple_input = maple_expression.toString();
        return translateFromMapleToLaTeX( maple_input );
    }

    /**
     * This method takes the given Maple expression and translates it to
     * the equivalent semantic LaTeX expression. This semantic LaTeX expression
     * will be translated back to a Maple expression again. It returns the
     * string representation of the new Maple expression. Ideally the input
     * and output strings are equivalent.
     *
     * @param maple_expression string of a Maple expression
     * @param label label of the latex expression (can be null if there is none)
     * @return string of a Maple expression after the translation process to LaTeX and back again
     * @throws TranslationException if the forward or backward translation failed
     * @throws MapleException if the program cannot evaluate the given expression
     */
    public String oneCycleRoundTripTranslationFromMaple( String maple_expression)
            throws TranslationException, MapleException {
        String latex = translateFromMapleToLaTeXClean( maple_expression );
        return translateFromLaTeXToMapleClean( latex, null );
    }

    /**
     * Does the same like {@link #oneCycleRoundTripTranslationFromMaple(String)}
     * but takes an {@link Algebraic} object and returns an {@link Algebraic} object.
     *
     * @param maple_expression algebraic object of a Maple expression
     * @param label label of the latex expression (can be null if there is none)
     * @return algebraic object of a Maple expression after the translation process to LaTeX and back again
     * @throws TranslationException if the forward or backward translation failed
     * @throws MapleException if the program cannot evaluate the given expression
     */
    public Algebraic oneCycleRoundTripTranslationFromMaple( Algebraic maple_expression )
            throws TranslationException, MapleException {
        String latex = translateFromMapleToLaTeXClean( maple_expression );
        return translateFromLaTeXToMapleAlgebraicClean( latex, null );
    }

    /**
     * This method takes the given semantic LaTeX expression and translates it to
     * the equivalent Maple expression. This Maple expression will be translated
     * back to a semantic LaTeX expression again. It returns the string representation
     * of the new semantic LaTeX expression. Ideally the input and output strings are
     * equivalent.
     *
     * @param latex_expression string of a semantic LaTeX expression
     * @param label label of the latex expression (can be null if there is none)
     * @return  string of a semantic LaTeX expression after the translation process
     *          to Maple and back again
     * @throws TranslationException if the forward or backward translation failed
     * @throws MapleException if Maple cannot evaluate an expression
     */
    public String oneCycleRoundTripTranslationFromLaTeX( String latex_expression, String label )
            throws TranslationException, MapleException {
        String maple = translateFromLaTeXToMapleClean( latex_expression, label );
        return translateFromMapleToLaTeXClean( maple );
    }

    /**
     * This method gives you direct access to the Maple kernel. Only use this method,
     * when you know what you do! The given expression must be a valid maple string
     * in Maple's 1-D representation!
     *
     * This method is useful to predefine variables, reset predefinitions, declare
     * assumptions are what else you want to do. In theory, it would be possible to
     * build your own translation program and use this method to get access to
     * the Maple kernel.
     *
     * Remember to reset defined variables if you don't want to use them any longer!
     *
     * @param mapleCommand a valid maple expression in Maple's 1D representation. (ending with ;)
     * @return the algebraic object of the result. Might be useful or can be ignored.
     * @throws MapleException if the given expression produces an error in Maple.
     */
    public Algebraic enterMapleCommand( String mapleCommand )
            throws MapleException {
        return mapleInterface.evaluateExpression(mapleCommand);
    }

    public void forceGC() throws ComputerAlgebraSystemEngineException {
        try {
            mapleInterface.invokeGC();
        } catch (MapleException me) {
            throw new ComputerAlgebraSystemEngineException(me);
        }
    }

    @Override
    public String buildList(List<String> list) {
        String listStr = MapleSimplifier.makeMapleList(list);
        if ( listStr != null && listStr.length() > 3 )
            return listStr.substring(1, listStr.length()-1);
        return listStr;
    }
}
