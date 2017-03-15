package gov.nist.drmf.interpreter;

import com.maplesoft.externalcall.MapleException;
import com.maplesoft.openmaple.Algebraic;
import gov.nist.drmf.interpreter.cas.translation.SemanticLatexTranslator;
import gov.nist.drmf.interpreter.common.GlobalConstants;
import gov.nist.drmf.interpreter.common.GlobalPaths;
import gov.nist.drmf.interpreter.common.Keys;
import gov.nist.drmf.interpreter.common.TranslationException;
import gov.nist.drmf.interpreter.maple.translation.MapleInterface;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Scanner;

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
public class MapleTranslator {
    /**
     * The logger.
     */
    private static final Logger LOG = LogManager.getLogger( MapleTranslator.class );

    /**
     * The interface to interact with the Maple translator
     */
    private MapleInterface mapleInterface;

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
    public static void main(String[] args){
        String test = "JacobiP(alpha, beta, n, cos(a*Theta))";

        MapleTranslator mt = new MapleTranslator();
        try {
            mt.init();
            Translation t = mt.translateFromMapleToLaTeX(test);
            System.out.println( "Translated:  " + t.getTranslatedExpression() );
            System.out.println( "Additional:  " + t.getAdditionalInformation() );
        } catch ( Exception e ){
            e.printStackTrace();
        }

        /*
        Scanner sc = new Scanner(System.in);
        if ( args != null && args.length >= 1 ){
            test = args[0];
        } else {
            System.out.println("Please enter a polynomial without functions in Maple representation.");
            test = sc.nextLine().trim();
            if ( test.matches("stop") ){
                System.out.println("You stopped the program.");
                return;
            }
        }
        */

        /*
        System.out.println("Initializing...");
        try {
            MapleTranslator t = new MapleTranslator();
            t.init();
            int c = 0;
            String tmp, last = "";
            LinkedList<String> latex_results = new LinkedList<>();
            LinkedList<String> maple_results = new LinkedList<>();
            boolean latex_equ = false, maple_equ = false;

            maple_results.add(test);

            while ( c < 10 && !(latex_equ && maple_equ) ){
                tmp = t.translateFromMapleToLaTeXClean( maple_results.getLast() );
                latex_results.addLast( tmp );
                if ( tmp.equals( last ) ) latex_equ = true;

                last = maple_results.getLast();
                tmp = t.translateFromLaTeXToMapleClean( latex_results.getLast() );
                maple_results.addLast( tmp );
                if ( tmp.equals( last ) ) maple_equ = true;

                last = latex_results.getLast();
                c++;
            }

            System.out.println("Fix-Point reached after " + c + " cycles.");
            System.out.println("Start equation: " + maple_results.removeFirst());
            System.out.println();

            for ( int i = 0; i < latex_results.size(); i++ ){
                System.out.println("Cycle: " + (i+1));
                System.out.println( latex_results.get(i) );
                System.out.println( maple_results.get(i) );
                System.out.println();
            }

            boolean b = t.simplificationTester( test, maple_results.getLast() );
            if ( b ){
                System.out.println("Both expressions are symbolical equivalent.");
            } else System.out.println("No equivalence found!");
        } catch ( Exception e ){
            e.printStackTrace();
        }
        */
    }

    /**
     * Creates an object of the translator class.
     * It is highly recommended to just instantiate one MapleTranslator
     * class at all!
     *
     * Before you start any translation process, you have to invoke
     * the {@link #init()} method once!
     */
    public MapleTranslator(){
        GlobalConstants.CAS_KEY = Keys.KEY_MAPLE;
    }

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
    public MapleTranslator(@Nonnull Path maple_dir ){
        this();
        try (FileOutputStream out =
                     new FileOutputStream( GlobalPaths.PATH_MAPLE_CONFIG.toFile() )){
            Properties props = new Properties();
            props.setProperty( Keys.KEY_MAPLE_BIN, maple_dir.toAbsolutePath().toString() );
            props.store(out, GlobalConstants.PROPS_COMMENTS);
            System.out.println("Finished to setup the properties file.");
        } catch ( IOException ioe ){
            System.err.println( "Cannot write the path into the properties file." );
            ioe.printStackTrace();
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

        dlmfInterface = new SemanticLatexTranslator( Keys.KEY_LATEX, Keys.KEY_MAPLE );
        dlmfInterface.init( GlobalPaths.PATH_REFERENCE_DATA );
        LOG.debug("Initialized DLMF LaTeX Interface.");
    }

    /**
     * Translates a given semantic LaTeX expression into an equivalent Maple string.
     * @param latex_expression expression in semantic LaTeX
     * @return equivalent expression in Maple syntax
     * @throws TranslationException if the translation fails.
     */
    public String translateFromLaTeXToMapleClean(@Nonnull String latex_expression )
            throws TranslationException{
        return translateFromLaTeXToMaple( latex_expression ).getTranslatedExpression();
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
     * @return equivalent expression in Maple syntax in a {@link Translation} object.
     * @throws TranslationException if the translation fails.
     */
    public Translation translateFromLaTeXToMaple(@Nonnull String latex_expression )
            throws TranslationException {
        dlmfInterface.translate( latex_expression );
        return new Translation(
                dlmfInterface.getGlobalExpressionObject().toString(),
                dlmfInterface.getInfoLog().toString() );
    }

    /**
     * Translates a given semantic LaTeX expression into an equivalent Maple expression
     * and returns the {@link Algebraic} object of the translated expression.
     *
     * @param latex_expression expression in semantic LaTeX
     * @return equivalent expression in Maple syntax as an {@link Algebraic} object.
     * @throws TranslationException if the translation fails.
     * @throws MapleException if the conversion into an {@link Algebraic} object fails.
     */
    public Algebraic translateFromLaTeXToMapleAlgebraicClean(@Nonnull String latex_expression )
            throws TranslationException, MapleException{
        return translateFromLaTeXToMapleAlgebraic( latex_expression ).getAlgebraicTranslatedExpression();
    }

    /**
     * Translates a given semantic LaTeX expression into an equivalent Maple expression
     * and returns the {@link Translation} object of the translated expression. This
     * object contains an {@link Algebraic} object of the translated expression also!
     *
     * @param latex_expression expression in semantic LaTeX
     * @return equivalent expression in Maple syntax as an {@link Translation} object.
     *          This object also contains the {@link Algebraic} object of the translated
     *          expression.
     * @throws TranslationException if the translation fails.
     * @throws MapleException if the conversion into an {@link Algebraic} object fails.
     */
    public Translation translateFromLaTeXToMapleAlgebraic(@Nonnull String latex_expression )
            throws TranslationException, MapleException {
        Translation t = translateFromLaTeXToMaple( latex_expression );
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
    public String translateFromMapleToLaTeXClean(@Nonnull String maple_expression )
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
    public Translation translateFromMapleToLaTeX(@Nonnull String maple_expression )
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
    public String translateFromMapleToLaTeXClean(@Nonnull Algebraic maple_expression )
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
    public Translation translateFromMapleToLaTeX(@Nonnull Algebraic maple_expression )
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
     * @return string of a Maple expression after the translation process to LaTeX and back again
     * @throws TranslationException if the forward or backward translation failed
     * @throws MapleException if the program cannot evaluate the given expression
     */
    public String oneCycleRoundTripTranslationFromMaple(@Nonnull String maple_expression )
            throws TranslationException, MapleException {
        String latex = translateFromMapleToLaTeXClean( maple_expression );
        return translateFromLaTeXToMapleClean( latex );
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
    public Algebraic oneCycleRoundTripTranslationFromMaple(@Nonnull Algebraic maple_expression )
            throws TranslationException, MapleException {
        String latex = translateFromMapleToLaTeXClean( maple_expression );
        return translateFromLaTeXToMapleAlgebraicClean( latex );
    }

    /**
     * This method takes the given semantic LaTeX expression and translates it to
     * the equivalent Maple expression. This Maple expression will be translated
     * back to a semantic LaTeX expression again. It returns the string representation
     * of the new semantic LaTeX expression. Ideally the input and output strings are
     * equivalent.
     *
     * @param latex_expression string of a semantic LaTeX expression
     * @return  string of a semantic LaTeX expression after the translation process
     *          to Maple and back again
     * @throws TranslationException if the forward or backward translation failed
     * @throws MapleException if Maple cannot evaluate an expression
     */
    public String oneCycleRoundTripTranslationFromLaTeX(@Nonnull String latex_expression )
            throws TranslationException, MapleException {
        String maple = translateFromLaTeXToMapleClean( latex_expression );
        return translateFromMapleToLaTeXClean( maple );
    }

    /**
     * This method takes two maple expressions and returns true when both expression
     * are symbolically the same. To verify this, we use the "simplify" command from
     * Maple. Be aware that both expressions still can be mathematically equivalent
     * even when this method returns false!
     *
     * Be also aware that null inputs always returns false, even when both inputs are null.
     * And two empty expression like "" and "" returns true.
     *
     * @param exp1 Maple string of the first expression
     * @param exp2 Maple string of the second expression
     * @return true if both expressions are symbolically equivalent or false otherwise.
     *          If it returns false, both expressions still can be mathematically equivalent!
     * @throws MapleException If the test of equivalence produces an Maple error.
     */
    public boolean simplificationTester(@Nullable String exp1, @Nullable String exp2 )
            throws MapleException {
        // test if one of the inputs is null
        if ( exp1 == null || exp2 == null ) return false;
        // if one of the expressions is empty, it only returns true when both are empty
        if ( exp1.isEmpty() || exp2.isEmpty() ){
            return exp1.isEmpty() && exp2.isEmpty();
        }

        // otherwise build simplify command to test equivalence
        String command = "simplify((" + exp1 + ") - (" + exp2 + "));";
        // log for debugging
        LOG.debug("Simplification-Test: " + command);

        // analyze the algebraic solution
        Algebraic solution = mapleInterface.evaluateExpression( command );
        // null solutions returns false
        if ( solution == null || solution.isNULL() ) return false;
        // analyze the output string and returns true when it matches "0".
        String solution_str = solution.toString();
        return solution_str.trim().matches("0");
    }
}