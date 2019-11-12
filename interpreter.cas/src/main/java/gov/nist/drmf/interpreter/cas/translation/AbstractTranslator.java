package gov.nist.drmf.interpreter.cas.translation;

import gov.nist.drmf.interpreter.cas.common.ForwardTranslationProcessConfig;
import gov.nist.drmf.interpreter.cas.common.IForwardTranslator;
import gov.nist.drmf.interpreter.cas.logging.TranslatedExpression;
import gov.nist.drmf.interpreter.cas.translation.components.*;
import gov.nist.drmf.interpreter.common.InformationLogger;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationExceptionReason;
import gov.nist.drmf.interpreter.common.grammar.Brackets;
import gov.nist.drmf.interpreter.common.grammar.ExpressionTags;
import gov.nist.drmf.interpreter.common.grammar.LimitedExpressions;
import gov.nist.drmf.interpreter.common.grammar.MathTermTags;
import gov.nist.drmf.interpreter.mlp.extensions.FeatureSetUtility;
import mlp.FeatureSet;
import mlp.MathTerm;
import mlp.PomTaggedExpression;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.regex.Matcher;

import static gov.nist.drmf.interpreter.cas.common.DLMFPatterns.*;

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
    private ForwardTranslationProcessConfig config;

    /**
     * The super translator object for handling global translations.
     */
    private final AbstractTranslator superTranslator;

    /**
     * Every translator has an abstract super translator object, which should be
     * unique for each translation process. Thus, this
     * @param superTranslator the super translator object
     */
    protected AbstractTranslator(AbstractTranslator superTranslator) {
        this.superTranslator = superTranslator;

        if ( superTranslator != null ) {
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
        } else {
            globalExp = new TranslatedExpression();
            infoLogger = new InformationLogger();
        }
    }

    @Override
    public abstract TranslatedExpression translate(PomTaggedExpression expression);

    /**
     * The main function of the abstract translator. This function
     * delegates a translation process to the specialized sub-translators.
     *
     * @param exp the current element
     * @param exp_list siblings of the current element (might be null)
     * @return the translated expression of this element
     */
    protected TranslatedExpression parseGeneralExpression(
            PomTaggedExpression exp,
            List<PomTaggedExpression> exp_list
    ) {
        TranslatedExpression transExpression;

        // accents are not supported
        if ( isAccented(exp) ){
            throw buildException(
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
            if (isDLMFMacro(term)) { // BEFORE FUNCTION!
                MacroTranslator mp = new MacroTranslator(this);
                transExpression = mp.translate(exp, exp_list);
            } //is it a sum or a product
            else if (isSumOrProductOrLimit(term)) {
                LimitedTranslator sm = new LimitedTranslator(this);
                transExpression = sm.translate(exp, exp_list);
            } // it could be a sub sequence
            else if (isSubSequence(term)) {
                Brackets bracket = Brackets.getBracket(term.getTermText());
                SequenceTranslator sp = new SequenceTranslator(this, bracket, SET_MODE);
                transExpression = sp.translate(exp_list);
            } // this is special, could be a function like cos
            else if (isFunction(term)) {
                FunctionTranslator fp = new FunctionTranslator(this);
                transExpression = fp.translate(exp, exp_list);
            } // otherwise it is a general math term
            else {
                MathTermTranslator mp = new MathTermTranslator(this);
                transExpression = mp.translate(exp, exp_list);
            }
        }

        LOG.trace("Global translation list: " + globalExp.debugString());
        return transExpression; //inner_parser.getTranslatedExpressionObject();
    }

    private TranslatedExpression saveParser(
            AbstractTranslator translator,
            boolean translateFollowingMode,
            PomTaggedExpression exp,
            List<PomTaggedExpression> expList
    ) {
        try {
            if ( translator instanceof AbstractListTranslator ){
                AbstractListTranslator alt = (AbstractListTranslator) translator;
                return translateFollowingMode ? alt.translate(exp, expList) : alt.translate(exp);
            } else {
                return translator.translate(exp);
            }
        } catch (TranslationException te) {
            if ( tolerant ) {
                LOG.warn("Error due to translation", te);
                return new TranslatedExpression();
            } else throw te;
        }
    }

    /**
     * A generic function that translates the next {@param expression} and cleans the the global translation list afterwards
     * @param expression translate expression
     * @param following_exps the following expressions
     * @return the translated expression
     */
    public TranslatedExpression translateInnerExp(
            PomTaggedExpression expression,
            List<PomTaggedExpression> following_exps
    ) {
        TranslatedExpression inner_exp =
                parseGeneralExpression(
                        expression,
                        following_exps
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
    private boolean isTaggedExpression(PomTaggedExpression e) {
        return !containsTerm(e) || isSQRT(e);
    }

    /**
     * Returns true if the expression does contain a math term
     * @param e expression
     * @return true if the expression contains a non-empty math term
     */
    protected boolean containsTerm(PomTaggedExpression e) {
        MathTerm t = e.getRoot();
        return t != null && !t.isEmpty();
    }

    private boolean isSQRT(PomTaggedExpression e) {
        String etag = e.getTag();
        if ( etag == null ) return false;
        ExpressionTags et = ExpressionTags.getTagByKey(etag);
        if ( et != null && (et.equals(ExpressionTags.square_root) || et.equals(ExpressionTags.general_root) ) ) {
            return true;
        } else return false;
    }

    protected boolean isDLMFMacro(MathTerm term) {
        MathTermTags tag = MathTermTags.getTagByKey(term.getTag());
        if (tag != null && tag.equals(MathTermTags.dlmf_macro)) {
            return true;
        }
        FeatureSet dlmf = term.getNamedFeatureSet(Keys.KEY_DLMF_MACRO);
        if (dlmf != null) {
            SortedSet<String> role = dlmf.getFeature(Keys.FEATURE_ROLE);
            if (role != null &&
                    (role.first().matches(Keys.FEATURE_VALUE_CONSTANT) ||
                            role.first().matches(Keys.FEATURE_VALUE_SYMBOL)
                    )) {
                return false;
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    private boolean isSumOrProductOrLimit(MathTerm term) {
        return LimitedExpressions.isLimitedExpression(term);
    }

    private boolean isSubSequence(MathTerm term) {
        String tag = term.getTag();
        if (tag != null && tag.matches(OPEN_PARENTHESIS_PATTERN)) {
            return true;
        } else if (tag != null && tag.matches(CLOSE_PARENTHESIS_PATTERN)) {
            LOG.error("Reached a closed bracket " + term.getTermText() +
                    " but there was not a corresponding" +
                    " open bracket before.");
            return false;
        } else {
            return false;
        }
    }

    private boolean isFunction(MathTerm term) {
        MathTermTags tag = MathTermTags.getTagByKey(term.getTag());
        if (tag == null) {
            return FeatureSetUtility.isFunction(term);
        }
        if (tag.equals(MathTermTags.function)) {
            return true;
        }
        return false;
    }

    /**
     * Sets the config for this abstract translator.
     * @param config configuration
     */
    void setConfig(ForwardTranslationProcessConfig config) {
        this.config = config;
    }

    /**
     * Gets the configuration
     * @return config
     */
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
        else return this.superTranslator.globalExp;
    }

    /**
     * Gets the super translator object, if any
     * @return super AbstractTranslator
     */
    protected AbstractTranslator getSuperTranslator() {
        return this.superTranslator;
    }

    public boolean isAccented( PomTaggedExpression pte ) {
        List<String> tags = pte.getSecondaryTags();
        for ( String t : tags ) {
            if ( t.matches(ExpressionTags.accented.tag()) ) {
                return true;
            }
        }

        MathTerm mt = pte.getRoot();
        if ( mt != null && !mt.isEmpty() ){
            List<String> mtags = mt.getSecondaryTags();
            for ( String t : mtags ) {
                if ( t.matches(ExpressionTags.accented.tag()) ) {
                    return true;
                }
            }
        }

        return false;
    }

    public void activateSetMode() {
        LOG.info("Set-Mode for sequences activated!");
        SET_MODE = true;
    }

    public void deactivateSetMode() {
        LOG.info("Set-Mode for sequences deactivated!");
        SET_MODE = false;
    }

    public boolean isMlpError() {
        return mlpError;
    }

    public void setTolerant(boolean tolerant) {
        this.tolerant = tolerant;
    }

    public void reset() {
        globalExp = new TranslatedExpression();
        mlpError = false;
        infoLogger = new InformationLogger();
    }

    protected void appendLocalErrorExpression(String tag) {
        LOG.debug("Adding fake Maple for error expression " + tag);
        globalExp.addTranslatedExpression("\"error" + StringEscapeUtils.escapeJava(tag) + "\"");
    }

    /**
     * Simple test if the given string is wrapped by parenthesis.
     * It only returns true if there is an open bracket at start and
     * at the end AND the first open one is really closed in the end.
     * Something like (1)/(2) would return false.
     *
     * @param str with or without brackets
     * @return false if there are no brackets
     */
    protected static boolean testBrackets(String str) {
        String tmp = str.trim();
        if (!tmp.matches(Brackets.OPEN_PATTERN + ".*" + Brackets.CLOSED_PATTERN)) {
            return false;
        }

        Brackets open = Brackets.getBracket(tmp.charAt(0) + "");
        Brackets inner, last;
        LinkedList<Brackets> open_list = new LinkedList<>();
        open_list.add(open);
        String symbol;

        for (int i = 1; i < tmp.length(); i++) {
            if (open_list.isEmpty()) {
                return false;
            }

            symbol = "" + tmp.charAt(i);
            inner = Brackets.getBracket(symbol);

            if (inner == null) {
                continue;
            } else if (inner.opened) {
                open_list.addLast(inner);
            } else {
                last = open_list.getLast();
                if (last.counterpart.equals(inner.symbol)) {
                    open_list.removeLast();
                } else {
                    return false;
                }
            }
        }
        return open_list.isEmpty();
    }

    public void setFileID(String fileID) {
        this.fileID = fileID;
    }

    /**
     * Moritz: this message makes the idea of error handling very difficult. Since the translators
     * are highly embedded in each other, the trace of the real error get lost due to this function.
     *
     * I recommend you implement error handling in your batch processor and not in the translator.
     *
     * @param message
     * @param reason
     * @return
     */
//    protected boolean handleNull(Object o, String message, Reason reason, String token, Exception exception) {
//        if (o == null) {
//            String exceptionString = "";
//            String location = "";
//            if (LOG.isWarnEnabled() && exception != null) {
//                try {
//                    final StackTraceElement[] stackTrace = exception.getStackTrace();
//                    final StackTraceElement traceElement = stackTrace[0];
//                    location = traceElement.getClassName() + ":L" + traceElement.getLineNumber();
//                    exceptionString = exception.getMessage();
//                } catch (Exception e) {
//                    //ignore
//                }
//            }
//            if (reason == Reason.MLP_ERROR) {
//                mlpError = true;
//            }
//            final String errorMessage = String.format(
//                    "Translation error in id '%s'\n\t" +
//                            "message:     %s,\n\t" +
//                            "token:       %s,\n\t" +
//                            "reason:      %s,\n\t" +
//                            "location:    %s,\n\t" +
//                            "translation: %s -> %s,\n\t" +
//                            "exception:   %s",
//                    this.fileID,
//                    message,
//                    token,
//                    reason,
//                    location,
//                    config.getFROM_LANGUAGE(),
//                    config.getTO_LANGUAGE(),
//                    exceptionString
//            );
//            LOG.warn(errorMessage);
//            final Matcher m = DLMF_ID_PATTERN.matcher(fileID);
//            if (m.matches()) {
//                final int chapter = Integer.parseInt(m.group(1));
//                if (!problemTokens.containsKey(token)) {
//                    problemTokens.put(token, new HashMap<>());
//                }
//                final Map<Integer, Set<String>> tokenMap = problemTokens.get(token);
//                if (!tokenMap.containsKey(chapter)) {
//                    tokenMap.put(chapter, new HashSet<>());
//                }
//                final Set<String> messages = tokenMap.get(chapter);
//                messages.add(errorMessage);
//            }
//            if (tolerant) {
//                appendLocalErrorExpression(token);
//                return true;
//            }
//            TranslationException te = new TranslationException(
//                    config.getFROM_LANGUAGE(),
//                    config.getTO_LANGUAGE(),
//                    message,
//                    reason,
//                    token,
//                    exception
//            );
//            LOG.error("Error due translation process.", te);
//            throw te;
//        }
//        return false;
//    }

    public TranslationException buildException( String message, TranslationExceptionReason reason ) {
        return new TranslationException(
                config.getFROM_LANGUAGE(),
                config.getTO_LANGUAGE(),
                message,
                reason
        );
    }

    public TranslationException buildException( String message, TranslationExceptionReason reason, Throwable throwable ) {
        return new TranslationException(
                config.getFROM_LANGUAGE(),
                config.getTO_LANGUAGE(),
                message,
                reason,
                throwable
        );
    }

    public Map<String, Map<Integer, Set<String>>> getProblemTokens() {
        return problemTokens;
    }
}
