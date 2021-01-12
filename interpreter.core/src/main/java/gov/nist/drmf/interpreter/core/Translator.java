package gov.nist.drmf.interpreter.core;

import com.maplesoft.externalcall.MapleException;
import com.maplesoft.openmaple.Algebraic;
import gov.nist.drmf.interpreter.cas.translation.SemanticLatexTranslator;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import gov.nist.drmf.interpreter.common.exceptions.InitTranslatorException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.maple.extension.MapleInterface;
import gov.nist.drmf.interpreter.maple.extension.Simplifier;
import gov.nist.drmf.interpreter.maple.translation.MapleTranslator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class can be used as an interface with the translation
 * programs between semantic LaTeX and Maple. It provides some
 * typical functions to translate expressions.
 *
 * It is recommended to only use one instance of this class. Take
 * the default instance via {@link #getDefaultInstance()}.
 *
 * @see Translation
 * @see Algebraic
 * @see MapleException
 * @see TranslationException
 * @see com.maplesoft.openmaple.Engine
 * @see gov.nist.drmf.interpreter.maple.translation.MapleTranslator
 * @see gov.nist.drmf.interpreter.cas.translation.SemanticLatexTranslator
 * @author Andre Greiner-Petter
 */
@SuppressWarnings("unused")
public class Translator {
    /**
     * Logging via Log4J2
     */
    private static final Logger LOG = LogManager.getLogger(Translator.class.getName());

    /**
     * Forward translators
     */
    private final SemanticLatexTranslator dlmfMapleInterface;
//    private final SemanticLatexTranslator dlmfMathematicaInterface;

    /**
     * Backward translators
     */
    private final MapleInterface mapleInterface;
    private final MapleTranslator mapleTranslator;
    private final Simplifier mapleSimplifier;

    /**
     * Default instance of this class
     */
    private static Translator translator;

    /**
     * Creates an object of the translator class. Note that Maple only
     * allows one running interface at a time. To avoid overlapping and
     * slow performance, it is recommended to use the default instance
     * of this class via {@link #getDefaultInstance()}.
     *
     * Maple also requires proper environment variables
     * MAPLE and LD_LIBRARY_PATH. See more about it in README.md
     */
    public Translator() throws InitTranslatorException {
        // setup logging
//        System.setProperty( Keys.KEY_SYSTEM_LOGGING, GlobalPaths.PATH_LOGGING_CONFIG.toString() );

        LOG.debug("Instantiate forward translation to Maple");
        dlmfMapleInterface = new SemanticLatexTranslator(Keys.KEY_MAPLE);

//        LOG.debug("Instantiate forward translation to Mathematica");
//        dlmfMathematicaInterface = new SemanticLatexTranslator(Keys.KEY_MATHEMATICA);
//        dlmfMathematicaInterface.init(GlobalPaths.PATH_REFERENCE_DATA);

        LOG.debug("Instantiate Maple's interface and backward translator");
        mapleInterface = MapleInterface.getUniqueMapleInterface();
        mapleTranslator = MapleTranslator.getDefaultInstance();
        mapleSimplifier = new Simplifier();
    }

    /**
     * Returns the default translator instance. If there is an error due
     * instantiation, it returns null and logs an error message.
     * @return the default translator instance or null if something went wrong
     */
    public static Translator getDefaultInstance() {
        if ( translator == null ) {
            try {
                translator = new Translator();
            } catch (InitTranslatorException e) {
                LOG.error("Cannot instantiate default translator", e);
                return null;
            }
        }
        return translator;
    }

    public SemanticLatexTranslator getDLMFToMapleTranslator() {
        return dlmfMapleInterface;
    }

    public MapleInterface getMapleInterface() {
        return mapleInterface;
    }

    public MapleTranslator getMapleTranslator() {
        return mapleTranslator;
    }

    public Simplifier getMapleSimplifier() {
        return mapleSimplifier;
    }

    /**
     * Restarts the maple session. This will clear the internal memory.
     * In consequence, you have to reload all custom scripts and set all
     * variables again. Try to avoid restarting the engine again and again!
     *
     * @throws MapleException if the restart throws an error
     */
    public void restartMapleSession() throws MapleException {
        mapleInterface.restart();
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
        dlmfMapleInterface.activateSetMode();
        String translation = translateFromLaTeXToMaple( latex_expression, label ).getTranslatedExpression();
        dlmfMapleInterface.deactivateSetMode();
        return translation;
    }

    /**
     * Translates a given semantic LaTeX expression into an equivalent Maple expression.
     * The {@link Translation} is a java bean to save the translated expression and
     * further information about the translation. This object doesn't contains an
     * {@link Algebraic} object of the translation. Use
     * {@link #translateFromLaTeXToMapleAlgebraicClean(String, String)}
     * or {@link #translateFromLaTeXToMapleAlgebraic(String, String)} to get this object.
     *
     * @param latex_expression expression in semantic LaTeX
     * @param label label of the latex expression (can be null if there is none)
     * @return equivalent expression in Maple syntax in a {@link Translation} object.
     * @throws TranslationException if the translation fails.
     */
    public Translation translateFromLaTeXToMaple(String latex_expression, String label )
            throws TranslationException {
        String translation = dlmfMapleInterface.translate( latex_expression, label );
        return new Translation(
                translation,
                dlmfMapleInterface.getInfoLogger().toString() );
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
        Algebraic a = mapleInterface.evaluate( "'" + t.getTranslatedExpression() + "'" );
        return new Translation( a, t.getTranslatedExpression(), t.getAdditionalInformation() );
    }

    /**
     * Translates a given Maple expression into the equivalent semantic LaTeX expression and
     * returns the string of this translated expression.
     *
     * @param maple_expression expression in Maple syntax
     * @return the equivalent semantic LaTeX expression.
     * @throws TranslationException if the translation failed
     */
    public String translateFromMapleToLaTeXClean( String maple_expression )
            throws TranslationException {
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
     */
    public Translation translateFromMapleToLaTeX( String maple_expression )
            throws TranslationException {
        String trans = mapleTranslator.translate( maple_expression );
        return new Translation( trans, mapleTranslator.getInfos().toString() );
    }

    /**
     * Translates a given Maple {@link Algebraic} object into the equivalent
     * semantic LaTeX expression and returns the string of this translated
     * expression.
     *
     * @param maple_expression algebraic object of a Maple expression
     * @return the equivalent semantic LaTeX expression
     * @throws TranslationException if the translation failed
     */
    public String translateFromMapleToLaTeXClean( Algebraic maple_expression )
            throws TranslationException {
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
     */
    public Translation translateFromMapleToLaTeX( Algebraic maple_expression )
            throws TranslationException {
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
     * @return string of a Maple expression after the translation process to LaTeX and back again
     * @throws TranslationException if the forward or backward translation failed
     */
    public String oneCycleRoundTripTranslationFromMaple( String maple_expression)
            throws TranslationException {
        String latex = translateFromMapleToLaTeXClean( maple_expression );
        return translateFromLaTeXToMapleClean( latex, null );
    }

    /**
     * Does the same like {@link #oneCycleRoundTripTranslationFromMaple(String)}
     * but takes an {@link Algebraic} object and returns an {@link Algebraic} object.
     *
     * @param maple_expression algebraic object of a Maple expression
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
     */
    public String oneCycleRoundTripTranslationFromLaTeX( String latex_expression, String label )
            throws TranslationException {
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
        return mapleInterface.evaluate(mapleCommand);
    }

    /**
     * This method takes two maple expressions and returns true when both expression
     * are symbolically the same. To verify this, we use the "simplify" command from
     * Maple. Be aware that both expressions still can be mathematically equivalent
     * even when this method returns false!
     *
     * Be also aware that null inputs always returns false, even when both inputs are null.
     * However, two empty expression such as "" and "" returns true.
     *
     * @param exp1 Maple string of the first expression
     * @param exp2 Maple string of the second expression
     * @return true if both expressions are symbolically equivalent or false otherwise.
     *          If it returns false, both expressions still can be mathematically equivalent!
     * @throws ComputerAlgebraSystemEngineException If the test of equivalence produces an Maple error.
     */
    public boolean isEquivalent( String exp1, String exp2 )
            throws ComputerAlgebraSystemEngineException {
        return mapleSimplifier.isEquivalent(exp1, exp2);
    }
}
