package gov.nist.drmf.interpreter.cas.translation;

import gov.nist.drmf.interpreter.cas.common.ForwardTranslationProcessConfig;
import gov.nist.drmf.interpreter.cas.common.IForwardTranslator;
import gov.nist.drmf.interpreter.cas.logging.TranslatedExpression;
import gov.nist.drmf.interpreter.cas.translation.components.*;
import gov.nist.drmf.interpreter.common.InformationLogger;
import gov.nist.drmf.interpreter.common.TranslationInformation;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationExceptionReason;
import gov.nist.drmf.interpreter.pom.common.grammar.Brackets;
import gov.nist.drmf.interpreter.pom.common.grammar.MathTermTags;
import gov.nist.drmf.interpreter.pom.common.MathTermUtility;
import gov.nist.drmf.interpreter.pom.common.PomTaggedExpressionUtility;
import mlp.MathTerm;
import mlp.PomTaggedExpression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.function.*;

/**
 * The abstract translator delegates the translation process to the specialized sub-translator
 * classes. All sub-translators must extend the {@link AbstractTranslator} or it's subclass
 * {@link AbstractListTranslator}.
 *
 * @author Andre Greiner-Petter
 */
public abstract class AbstractTranslator implements IForwardTranslator {
    /**
     * The logger, only on abstract level
     */
    private static final Logger LOG = LogManager.getLogger(AbstractTranslator.class.getName());

    /**
     * Store problematic tokens for error analysis
     */
    private final Map<String, Map<Integer, Set<String>>> problemTokens = new HashMap<>();

    /**
     * The translator may work on a specific file -> this is the fileID.
     * It is undefined if there is no such file.
     */
    private String fileID = "undefined";

    /**
     * The global information logger
     */
    private InformationLogger infoLogger;

    /**
     * The global translated expression list
     */
    private TranslatedExpression globalExp;

    /**
     * In case of equation arrays or multi-case expressions, we most likely end up
     * with additional global expressions.
     */
    private List<TranslatedExpression> partialTranslations;

    /**
     * The translation information object. This is unique for one translation
     * process.
     */
    private TranslationInformation translationInformation;

    /**
     * Flags of each translator
     */
    private boolean SET_MODE    = false;
    private boolean mlpError    = false;

    /**
     * Tolerant mode means, an error gets ignored
     */
    private boolean tolerant    = false;

    /**
     * The current forward translation config
     */
    private final ForwardTranslationProcessConfig config;

    /**
     * The super translator object for handling global translations.
     */
    private final AbstractTranslator superTranslator;

    /**
     * The initialization constructor. This must be only invoked once for each translation process.
     * Due a translation process, one must use {@link #AbstractTranslator(AbstractTranslator)} to set
     * the unique super translator that handles the translated expressions lists.
     * @param config configuration
     */
    AbstractTranslator(ForwardTranslationProcessConfig config) {
        this.superTranslator = null;
        this.config = config;
        this.globalExp = new TranslatedExpression();
        this.infoLogger = new InformationLogger();
        this.partialTranslations = new LinkedList<>();
        this.translationInformation = new TranslationInformation();
    }

    /**
     * Every translator has an abstract super translator object, which should be
     * unique for each translation process. For initialization of a complete new
     * independent translation process, use {@link #AbstractTranslator(ForwardTranslationProcessConfig)}.
     *
     * @param superTranslator the super translator object
     * @throws NullPointerException if the provided super translator is null. For initialization
     * one must use {@link #AbstractTranslator(ForwardTranslationProcessConfig)}.
     */
    protected AbstractTranslator(AbstractTranslator superTranslator) throws NullPointerException {
        this.superTranslator = superTranslator;

        // the following objects are shared among all translators
        this.config = superTranslator.config;

        if ( superTranslator.globalExp == null ) {
            superTranslator.globalExp = new TranslatedExpression();
        }
        this.globalExp = superTranslator.globalExp;

        if ( superTranslator.infoLogger == null ) {
            superTranslator.infoLogger = new InformationLogger();
        }
        this.infoLogger = superTranslator.infoLogger;

        if ( superTranslator.partialTranslations == null ) {
            superTranslator.partialTranslations = new LinkedList<>();
        }
        this.partialTranslations = superTranslator.partialTranslations;

        if ( superTranslator.translationInformation == null ) {
            superTranslator.translationInformation = new TranslationInformation();
        }
        this.translationInformation = superTranslator.translationInformation;

        this.SET_MODE = superTranslator.SET_MODE;
        this.tolerant = superTranslator.tolerant;
        this.mlpError = superTranslator.mlpError;
    }

    @Override
    public abstract TranslatedExpression translate(PomTaggedExpression expression);

    /**
     * The main function of the abstract translator. This function
     * delegates a translation process to the specialized sub-translators.
     *
     * @param exp the current element
     * @param expList siblings of the current element (might be null)
     * @return the translated expression of this element
     */
    protected TranslatedExpression parseGeneralExpression(
            PomTaggedExpression exp,
            List<PomTaggedExpression> expList
    ) {
        TranslatedExpression transExpression;

        // accents are not supported
        if (PomTaggedExpressionUtility.isAccented(exp) ){
            throw TranslationException.buildException(
                    this,
                    "Accents are not supported.",
                    TranslationExceptionReason.MISSING_TRANSLATION_INFORMATION);
        }

        // if it is an empty exp, we are done
        if (exp.isEmpty()) {
            return globalExp;
        }

        // handle all different cases
        // first, does this expression contains a term?
        if (isTaggedExpression(exp)) {
            // the new version of sqrts are non-empty expressions but
            // can be considered as empty
            TaggedExpressionTranslator tet = new TaggedExpressionTranslator(this);
            transExpression = tet.translate(exp);
        } else { // if not handle all different cases of terms
            MathTerm term = exp.getRoot();
            // first, is this a DLMF macro?
            transExpression = parseGeneralTerm(term, exp, expList);
        }

        LOG.trace("Global translation list: " + globalExp.debugString());
        return transExpression; //inner_parser.getTranslatedExpressionObject();
    }

    private TranslatedExpression parseGeneralTerm(MathTerm term, PomTaggedExpression exp, List<PomTaggedExpression> expList) {
        TranslatedExpression transExpression;
        if (MathTermUtility.isDLMFMacro(term)) { // BEFORE FUNCTION!
            MacroTranslator mp = new MacroTranslator(this);
            transExpression = mp.translate(exp, expList);
        } //is it a sum or a product
        else if (MathTermUtility.isSumOrProductOrLimit(term)) {
            LimitedTranslator sm = new LimitedTranslator(this);
            transExpression = sm.translate(exp, expList);
        } // it could be a sub sequence
        else if (isSubSequence(term)) {
            Brackets bracket = Brackets.getBracket(term.getTermText());
            SequenceTranslator sp = new SequenceTranslator(this, bracket);
            transExpression = sp.translate(null, expList);
        } // this is special, could be a function like cos
        else if (MathTermUtility.isFunction(term)) {
            FunctionTranslator fp = new FunctionTranslator(this);
            transExpression = fp.translate(exp, expList);
        } // otherwise it is a general math term
        else {
            MathTermTranslator mp = new MathTermTranslator(this);
            transExpression = mp.translate(exp, expList);
        }
        return transExpression;
    }

    /**
     * A generic function that translates the next {@param expression} and cleans the the global translation list afterwards
     * @param expression translate expression
     * @param followingExps the following expressions
     * @return the translated expression
     */
    public TranslatedExpression translateInnerExp(
            PomTaggedExpression expression,
            List<PomTaggedExpression> followingExps
    ) {
        TranslatedExpression inner_exp =
                parseGeneralExpression(
                        expression,
                        followingExps
                );
        getGlobalTranslationList().removeLastNExps(inner_exp.getLength());
        return inner_exp;
    }

    /**
     * Returns true if the given expression is tagged expression. That means the
     * given PomTaggedExpression is either empty (no MathTerm) or is a square root.
     * Square roots (and general roots) are currently the only tokens that are not
     * empty but organized similar to empty expressions.
     *
     * @param e the expression
     * @return true if the expression is a tagged super expression
     */
    protected static boolean isTaggedExpression(PomTaggedExpression e) {
        return !containsTerm(e) || PomTaggedExpressionUtility.isSQRT(e);
    }

    /**
     * Returns true if the expression does contain a math term
     * @param e expression
     * @return true if the expression contains a non-empty math term
     */
    protected static boolean containsTerm(PomTaggedExpression e) {
        MathTerm t = e.getRoot();
        return t != null && !t.isEmpty();
    }

    protected static boolean isSubSequence(MathTerm term) {
        String tag = term.getTag();
        if (tag != null && tag.matches(MathTermTags.OPEN_PARENTHESIS_PATTERN)) {
            return true;
        } else if (tag != null && tag.matches(MathTermTags.CLOSE_PARENTHESIS_PATTERN)) {
            LOG.error("Reached a closed bracket " + term.getTermText() +
                    " but there was not a corresponding" +
                    " open bracket before.");
            return false;
        } else {
            return false;
        }
    }

    @Override
    public ForwardTranslationProcessConfig getConfig() {
        return this.config;
    }

    /**
     * Gets the information logger for the forward translation
     * @return information logger
     */
    protected InformationLogger getInfoLogger() {
        return this.infoLogger;
    }

    /**
     * Gets the global translated expression
     * @return global translated expression
     */
    protected TranslatedExpression getGlobalTranslationList() {
        if ( superTranslator == null ) return globalExp;
        else return this.superTranslator.getGlobalTranslationList();
    }

    /**
     * Adds a partial translated expression to the list.
     * @param translatedExpression will be copied so its save to use references
     */
    protected void addPartialTranslation(TranslatedExpression translatedExpression) {
        if ( superTranslator == null ) this.partialTranslations.add(new TranslatedExpression(translatedExpression));
        else superTranslator.addPartialTranslation(translatedExpression);
    }

    /**
     * The additional translations list
     * @return the list of additional translations
     */
    protected List<TranslatedExpression> getListOfPartialTranslations() {
        if ( superTranslator == null ) return this.partialTranslations;
        else return this.superTranslator.getListOfPartialTranslations();
    }

    /**
     * Get the unique translation information for this translation process.
     * @return the unique translation information for a single translation process
     */
    protected TranslationInformation getTranslationInformation() {
        if ( superTranslator == null ) return this.translationInformation;
        else return this.superTranslator.getTranslationInformation();
    }

    /**
     * Gets the super translator object, if any
     * @return super AbstractTranslator
     */
    protected AbstractTranslator getSuperTranslator() {
        return this.superTranslator;
    }

    public void activateSetMode() {
        LOG.debug("Set-Mode for sequences activated!");
        SET_MODE = true;
        if ( this.superTranslator != null ) this.superTranslator.activateSetMode();
    }

    public void deactivateSetMode() {
        LOG.debug("Set-Mode for sequences deactivated!");
        SET_MODE = false;
        if ( this.superTranslator != null ) this.superTranslator.deactivateSetMode();
    }

    protected boolean isSetMode() {
        return SET_MODE;
    }

    public boolean isMlpError() {
        return mlpError;
    }

    public void setTolerant(boolean tolerant) {
        this.tolerant = tolerant;
    }

    public void reset() {
        globalExp = new TranslatedExpression();
        partialTranslations = new LinkedList<>();
        infoLogger = new InformationLogger();
        translationInformation = new TranslationInformation();
        mlpError = false;
        SET_MODE = false;
        if ( this.superTranslator != null ) this.superTranslator.reset();
    }

    public void setFileID(String fileID) {
        this.fileID = fileID;
    }

    /**
     * This method is a short version to add an entry to the given local
     * {@link TranslatedExpression} object {@param local} and the global
     * {@link TranslatedExpression} object {@link #getGlobalTranslationList()}.
     *
     * For example, consider you want to add a string via {@link TranslatedExpression#addTranslatedExpression(String)}
     * to your local and global lists. Than you can do this via
     * <pre>
     *     addLocalAndGlobal(
     *          TranslatedExpression::addTranslatedExpression,
     *          localTranslations,
     *          output
     *     );
     * </pre>
     * which is equivalent to
     * <pre>
     *     localTranslations.addTranslatedExpression(output);
     *     getGlobalTranslationList().addTranslatedExpression(output);
     * </pre>
     *
     * @param consumer the method to invoke on {@link TranslatedExpression}
     * @param value the value to apply
     * @param <S> the type of the value to apply
     */
    protected <S> void perform(
            BiConsumer<? super TranslatedExpression, S> consumer,
            S value
    ) {
        consumer.accept(getTranslatedExpressionObject(), value);
        consumer.accept(getGlobalTranslationList(), value);
    }

    /**
     * This is the same as {@link #perform(BiConsumer, Object)} but before
     * applying the value {@param value}, we perform a mapping via {@param map}. For example, if you
     * want to add variables to the local and global {@link TranslatedExpression} object you can do this via
     * <pre>
     * addLocalAndGlobalMap(
     *      TranslatedExpression::getFreeVariables,
     *      FreeVariables::addFreeVariable,
     *      localTranslations,
     *      var
     * );
     * </pre>
     * which is again equivalent to
     * <pre>
     *     localTranslations.getFreeVariables().addFreeVariable(var);
     *     getGlobalTranslationList().getFreeVariables().addFreeVariable(var);
     * </pre>
     *
     * @param map the mapping that will be performed on the {@link TranslatedExpression} objects
     * @param consumer the method to invoke on the mapped objects
     * @param value the value to apply on the mapped elements methods
     * @param <S> the type of the value to apply
     * @param <T> the type of the mapped object
     */
    protected <S, T> void mapPerform(
            Function<? super TranslatedExpression, T> map,
            BiConsumer<? super T, S> consumer,
            S value
    ) {
        T mapped = map.apply(getTranslatedExpressionObject());
        T mapped2 = map.apply(getGlobalTranslationList());
        consumer.accept(mapped, value);
        consumer.accept(mapped2, value);
    }

    public Map<String, Map<Integer, Set<String>>> getProblemTokens() {
        return problemTokens;
    }
}