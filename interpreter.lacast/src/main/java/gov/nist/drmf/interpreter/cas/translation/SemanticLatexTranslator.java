package gov.nist.drmf.interpreter.cas.translation;

import gov.nist.drmf.interpreter.cas.blueprints.BlueprintMaster;
import gov.nist.drmf.interpreter.cas.common.ForwardTranslationProcessConfig;
import gov.nist.drmf.interpreter.cas.logging.TranslatedExpression;
import gov.nist.drmf.interpreter.common.InformationLogger;
import gov.nist.drmf.interpreter.common.TeXPreProcessor;
import gov.nist.drmf.interpreter.common.TranslationInformation;
import gov.nist.drmf.interpreter.common.cas.PackageWrapper;
import gov.nist.drmf.interpreter.common.exceptions.InitTranslatorException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationExceptionReason;
import gov.nist.drmf.interpreter.common.interfaces.IDLMFTranslator;
import gov.nist.drmf.interpreter.common.replacements.ConditionalReplacementRule;
import gov.nist.drmf.interpreter.common.replacements.IReplacementCondition;
import gov.nist.drmf.interpreter.pom.SemanticMLPWrapper;
import mlp.ParseException;
import mlp.PomTaggedExpression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * <p>
 * This class is the high-level entry point to perform translations from semantic LaTeX to CAS.
 * The translator is not thread-safe. To run translations in parallel, you must initiate multiple
 * independent instances.
 * </p>
 *
 * <p>
 * It provides multiple entry points to perform translations. Mainly
 * {@link #translate(String)} and {@link #translate(PomTaggedExpression)} to perform
 * translations directly on the given input. The former will call the internal
 * LaTeX parser {@link mlp.PomParser} to generate a {@link PomTaggedExpression}. The
 * second method simply translates the provided parsed expression. If you are not sure
 * what to use, it is always recommended to use the former {@link #translate(String)}
 * method.
 * </p>
 *
 * <p>
 * In addition, and in accordance to the DLMF, the class also provides the method
 * {@link #translate(String, String)} that performs pre-processing on the TeX string
 * according to the provided DLMF label. For example, every {@code i} in section 4
 * of the DLMF is the imaginary unit and hence, should be replaced by {@code \iunit}.
 * By providing the equation label, e.g., "4.4.4" referring to https://dlmf.nist.gov/4.4.4,
 * will pre-process the input {@code e^0 = 1} to {@code \expe^0 = 1}.
 * </p>
 *
 * @see mlp.PomParser
 * @author Andre Greiner-Petter
 */
public class SemanticLatexTranslator extends AbstractTranslator implements IDLMFTranslator {
    private static final Logger LOG = LogManager.getLogger(SemanticLatexTranslator.class.getName());

    /**
     * The latex parser
     */
    private SemanticMLPWrapper parser;

    private final ForwardTranslationProcessConfig config;

    private TranslatedExpression localTranslations;

    private String latestTranslationExpression;

    /**
     * Creates a forward translator to the specified language.
     * @param to_language the language key
     * @see gov.nist.drmf.interpreter.common.constants.Keys
     */
    public SemanticLatexTranslator( String to_language ) throws InitTranslatorException {
        this( new ForwardTranslationProcessConfig(to_language) );
    }

    /**
     * Creates a forward translator based on a the given config.
     * If the config does not contain a {@link BlueprintMaster} object
     * yet, it will be initialized in the {@link } function
     * and added to the config object.
     *
     * @param config config (with or without BlueprintMaster object)
     * @see BlueprintMaster
     */
    public SemanticLatexTranslator( ForwardTranslationProcessConfig config ) throws InitTranslatorException {
        super( config );
        this.config = config;
        this.localTranslations = new TranslatedExpression();
        this.init();
    }

    /**
     * Copy constructor
     * @param orig the original translator
     */
    private SemanticLatexTranslator( SemanticLatexTranslator orig ) throws InitTranslatorException {
        super( orig.getConfig() );
        this.config = orig.config;
        this.parser = orig.parser;
        this.localTranslations = new TranslatedExpression();
        this.latestTranslationExpression = orig.latestTranslationExpression;
        this.init();
    }

    /**
     * Initializes the back end for the translation from semantic LaTeX to
     * a computer algebra system. It loads all translation information
     * from the files in the given path and instantiate the PomParser from
     * Prof. Abdou Youssef.
     *
     * Note, that if the config did not contain a {@link BlueprintMaster}
     * object, it will be initialized and added to the config automatically.
     *
     * @throws InitTranslatorException if it is not possible to read the information
     *                      from the files.
     */
    private void init() throws InitTranslatorException {
        config.init();
        parser = SemanticMLPWrapper.getStandardInstance();
    }

    /**
     * Translates a given string to the another language.
     * @param expression the expression that should get translated.
     * @return the translated expression.
     * @throws TranslationException if an error occurred due translation
     */
    @Override
    public synchronized String translate( String expression ) throws TranslationException {
        return translate(expression, null);
    }

    /**
     * Translates a given string to the another language with a given label.
     * The label is used to specify additional pre-processing steps.
     *
     * @param expression the input expression (will be pre-processed)
     * @param label specifies the label of the input expression. This trigger additional
     *              pre-processing steps if the label matches defined replacement rules.
     * @return the translated expression
     * @throws TranslationException if an error occurred due translation
     *
     * @see TeXPreProcessor
     * @see ConditionalReplacementRule
     * @see IReplacementCondition
     */
    @Override
    public synchronized String translate( String expression, String label ) throws TranslationException {
        if ( expression == null || expression.isEmpty() ) {
            LOG.warn("Tried to translate an empty expression");
            return "";
        }

        return innerTranslate(expression, label);
    }

    /**
     * Translates a given string to the another language.
     * @param expression the expression that should get translated.
     * @return the translated expression.
     * @throws TranslationException if an error occurred due translation
     */
    @Override
    public synchronized TranslationInformation translateToObject( String expression ) throws TranslationException {
        return translateToObject(expression, null);
    }

    @Override
    public synchronized TranslationInformation translateToObject( String expression, String label ) throws TranslationException {
        if ( expression == null || expression.isEmpty() ) {
            LOG.warn("Tried to translate an empty expression");
            return null;
        }

        innerTranslate(expression, label);
        return getTranslationInformationObject();
    }

    public TranslationInformation getTranslationInformationObject() {
        TranslationInformation ti = new TranslationInformation(
                this.latestTranslationExpression,
                getGlobalTranslationList().getTranslatedExpression()
        );
        ti.setInformation(getInfoLogger());
        ti.setRequiredPackages(getTranslatedExpressionObject().getRequiredPackages());
        ti.addTranslatedConstraints(getTranslatedExpressionObject().getConstraints());
        for ( TranslatedExpression te : getListOfPartialTranslations() ) {
            TranslationInformation t = new TranslationInformation(this.latestTranslationExpression, te.getTranslatedExpression());
            t.setInformation(getInfoLogger());
            t.setRequiredPackages(te.getRequiredPackages());
            t.addTranslatedConstraints(te.getConstraints());
            ti.addTranslations(t);
        }
        return ti;
    }

    private String innerTranslate( String expression, String label ) throws TranslationException {
        try {
            this.latestTranslationExpression = expression;
            PomTaggedExpression exp = parser.parse(expression, label);
            translate(exp);
            if ( !super.getInfoLogger().isEmpty() && !config.shortenedOutput() ) {
                LOG.info(super.getInfoLogger().toString());
            }
            return getTranslatedExpression();
        } catch ( ParseException pe ){
            throw TranslationException.buildException(
                    this,
                    pe.getMessage(),
                    TranslationExceptionReason.MLP_ERROR,
                    pe
            );
        } catch ( TranslationException te ) {
            LOG.error("Unable to translate " + expression + ";\nReason: " + te.toString());
            throw te;
        }
    }

    @Override
    public String getTranslatedExpression() {
        if ( config.isInlinePackageMode() ) {
            PackageWrapper pw = new PackageWrapper(config);
            return super.getGlobalTranslationList().getTranslatedExpression(pw);
        } return super.getGlobalTranslationList().getTranslatedExpression();
    }

    @Override
    public synchronized TranslatedExpression translate( PomTaggedExpression expression ) throws TranslationException {
        reset();
        localTranslations = new TranslatedExpression();
        TranslatedExpression global = super.getGlobalTranslationList();

        parseGeneralExpression(expression, null);

        localTranslations.clear();
        localTranslations.addTranslatedExpression(global);
        localTranslations.addRequiredPackages(global.getRequiredPackages());

        return localTranslations;
    }

    @Override
    public InformationLogger getInfoLogger(){
        return super.getInfoLogger();
    }

    @Override
    public TranslatedExpression getTranslatedExpressionObject() {
        return localTranslations;
    }

    public BlueprintMaster getBlueprintMaster() {
        try {
            return config.getLimitParser();
        } catch (InitTranslatorException e) {
            LOG.fatal("Unable to load blueprint parser.");
            return null;
        }
    }
}
