package gov.nist.drmf.interpreter.cas.translation;

import gov.nist.drmf.interpreter.cas.common.ForwardTranslationProcessConfig;
import gov.nist.drmf.interpreter.cas.common.IForwardTranslator;
import gov.nist.drmf.interpreter.cas.logging.TranslatedExpression;
import gov.nist.drmf.interpreter.cas.translation.components.*;
import gov.nist.drmf.interpreter.common.InformationLogger;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException.Reason;
import gov.nist.drmf.interpreter.common.grammar.Brackets;
import gov.nist.drmf.interpreter.common.grammar.ITranslator;
import gov.nist.drmf.interpreter.common.grammar.MathTermTags;
import gov.nist.drmf.interpreter.mlp.extensions.FeatureSetUtility;
import mlp.FeatureSet;
import mlp.MathTerm;
import mlp.PomTaggedExpression;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.*;
import java.util.regex.Matcher;

import static gov.nist.drmf.interpreter.cas.common.DLMFPatterns.CLOSE_PARENTHESIS_PATTERN;
import static gov.nist.drmf.interpreter.cas.common.DLMFPatterns.DLMF_ID_PATTERN;
import static gov.nist.drmf.interpreter.cas.common.DLMFPatterns.OPEN_PARENTHESIS_PATTERN;

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
    private TranslatedExpression global_exp;

    /**
     * Every translator has it's own local translator.
     */
//    private TranslatedExpression local_inner_exp = new TranslatedExpression();

    /**
     * Flags of each translator
     */
    private boolean SET_MODE    = false;
    private boolean inner_Error = false;
    private boolean tolerant    = true;
    private boolean mlpError    = false;

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
     * @param superTranslator
     */
    protected AbstractTranslator(AbstractTranslator superTranslator) {
        this.superTranslator = superTranslator;

        if ( superTranslator != null ) {
            // the following objects are shared among all translators
            this.config = superTranslator.config;

            if ( superTranslator.global_exp == null ) {
                superTranslator.global_exp = new TranslatedExpression();
            }
            this.global_exp = superTranslator.global_exp;

            if ( superTranslator.infoLogger == null ) {
                superTranslator.infoLogger = new InformationLogger();
            }
            this.infoLogger = superTranslator.infoLogger;
        } else {
            global_exp = new TranslatedExpression();
            infoLogger = new InformationLogger();
        }
    }

    @Override
    public abstract boolean translate(PomTaggedExpression expression);

    /**
     * The main function of the abstract translator. This function
     * delegates a translation process to the specialized sub-translators.
     *
     * @param exp the current element
     * @param exp_list siblings of the current element (might be null)
     * @return the translated expression of this element
     */
    protected TranslatedExpression parseGeneralExpression(PomTaggedExpression exp, List<PomTaggedExpression> exp_list) {
        // create inner local translation (recursive)
        AbstractTranslator inner_parser = null;
        // if there was an inner error
        boolean return_value;

        // if it is an empty exp
        if (exp.isEmpty()) {
            return global_exp;
        }

        // handle all different cases
        // first, does this expression contains a term?
        if (!containsTerm(exp)) {
            inner_parser = new EmptyExpressionTranslator(this);
            return_value = inner_parser.translate(exp);
        } else { // if not handle all different cases of terms
            MathTerm term = exp.getRoot();
            // first, is this a DLMF macro?
            if (isDLMFMacro(term)) { // BEFORE FUNCTION!
                MacroTranslator mp = new MacroTranslator(this);
                return_value = mp.translate(exp, exp_list);
                inner_parser = mp;
            } //is it a sum or a product
            else if (isSumOrProductOrLimit(term)) {
                SumProductTranslator sm = new SumProductTranslator(this);
                return_value = sm.translate(exp, exp_list);
                inner_parser = sm;
            } // it could be a sub sequence
            else if (isSubSequence(term)) {
                Brackets bracket = Brackets.getBracket(term.getTermText());
                SequenceTranslator sp = new SequenceTranslator(this, bracket, SET_MODE);
                return_value = sp.translate(exp_list);
                inner_parser = sp;
            } // this is special, could be a function like cos
            else if (isFunction(term)) {
                FunctionTranslator fp = new FunctionTranslator(this);
                return_value = fp.translate(exp, exp_list);
                inner_parser = fp;
            } // otherwise it is a general math term
            else {
                MathTermTranslator mp = new MathTermTranslator(this);
                return_value = mp.translate(exp, exp_list);
                inner_parser = mp;
            }
        }

        inner_Error = !return_value;
        return inner_parser.getTranslatedExpressionObject();
    }

    protected void setConfig(ForwardTranslationProcessConfig config) {
        this.config = config;
    }

    protected ForwardTranslationProcessConfig getConfig() {
        return this.config;
    }

    protected InformationLogger getInfoLogger() {
        return this.infoLogger;
    }

    protected TranslatedExpression getGlobalTranslationList() {
        if ( superTranslator == null ) return global_exp;
        else return this.superTranslator.global_exp;
    }

    protected AbstractTranslator getSuperTranslator() {
        return this.superTranslator;
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

    protected boolean isSumOrProductOrLimit(MathTerm term) {
        if (term.getTag().equals(MathTermTags.operator.tag())) {
            return FeatureSetUtility.isSum(term) || FeatureSetUtility.isProduct(term) || FeatureSetUtility.isLimit(term);
        }
        return false;
    }

    protected boolean isSubSequence(MathTerm term) {
        String tag = term.getTag();
        if (tag.matches(OPEN_PARENTHESIS_PATTERN)) {
            return true;
        } else if (tag.matches(CLOSE_PARENTHESIS_PATTERN)) {
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

    public boolean containsTerm(PomTaggedExpression e) {
        MathTerm t = e.getRoot();
        return (t != null && !t.isEmpty());
    }

    protected boolean isInnerError() {
        return inner_Error;
    }

    public void reset() {
        global_exp = new TranslatedExpression();
        mlpError = false;
    }

    protected void appendLocalErrorExpression(String tag) {
        LOG.debug("Adding fake Maple for error expression " + tag);
        global_exp.addTranslatedExpression("\"error" + StringEscapeUtils.escapeJava(tag) + "\"");
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

    protected boolean handleNull(Object o, String message, Reason reason, String token, Exception exception) {
        if (o == null) {
            String exceptionString = "";
            if (LOG.isWarnEnabled() && exception != null) {
                try {
                    final StackTraceElement[] stackTrace = exception.getStackTrace();
                    final StackTraceElement traceElement = stackTrace[0];
                    exceptionString = traceElement.getClassName() + ":L"
                            + traceElement.getLineNumber() + ":" + exception.getMessage();
                } catch (Exception e) {
                    //ignore
                }
            }
            if (reason == Reason.MLP_ERROR) {
                mlpError = true;
            }
            final String errorMessage = String.format(
                    "Translation error in id '%s'\n\tmessage:%s\n\ttoken:%s\n\treason:%s,\n\texception:%s",
                    this.fileID, message, token, reason, exceptionString);
            LOG.warn(errorMessage);
            final Matcher m = DLMF_ID_PATTERN.matcher(fileID);
            if (m.matches()) {
                final int chapter = Integer.parseInt(m.group(1));
                if (!problemTokens.containsKey(token)) {
                    problemTokens.put(token, new HashMap<>());
                }
                final Map<Integer, Set<String>> tokenMap = problemTokens.get(token);
                if (!tokenMap.containsKey(chapter)) {
                    tokenMap.put(chapter, new HashSet<>());
                }
                final Set<String> messages = tokenMap.get(chapter);
                messages.add(errorMessage);
            }
            if (tolerant) {
                appendLocalErrorExpression(token);
                return true;
            }
            throw new TranslationException(
                    message,
                    reason,
                    token,
                    exception
            );
        }
        return false;
    }

    public Map<String, Map<Integer, Set<String>>> getProblemTokens() {
        return problemTokens;
    }
}
