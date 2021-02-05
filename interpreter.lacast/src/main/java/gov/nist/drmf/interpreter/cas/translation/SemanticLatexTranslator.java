package gov.nist.drmf.interpreter.cas.translation;

import gov.nist.drmf.interpreter.cas.blueprints.BlueprintMaster;
import gov.nist.drmf.interpreter.cas.common.ForwardTranslationProcessConfig;
import gov.nist.drmf.interpreter.cas.logging.TranslatedExpression;
import gov.nist.drmf.interpreter.cas.logging.TranslatedExpressionHelper;
import gov.nist.drmf.interpreter.common.InformationLogger;
import gov.nist.drmf.interpreter.common.interfaces.TranslationFeature;
import gov.nist.drmf.interpreter.common.latex.RelationalComponents;
import gov.nist.drmf.interpreter.common.latex.TeXPreProcessor;
import gov.nist.drmf.interpreter.common.TranslationInformation;
import gov.nist.drmf.interpreter.common.cas.PackageWrapper;
import gov.nist.drmf.interpreter.common.exceptions.InitTranslatorException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationExceptionReason;
import gov.nist.drmf.interpreter.common.interfaces.IDLMFTranslator;
import gov.nist.drmf.interpreter.common.replacements.ConditionalReplacementRule;
import gov.nist.drmf.interpreter.common.replacements.IReplacementCondition;
import gov.nist.drmf.interpreter.pom.SemanticMLPWrapper;
import gov.nist.drmf.interpreter.pom.common.PomTaggedExpressionNormalizer;
import gov.nist.drmf.interpreter.pom.extensions.PrintablePomTaggedExpression;
import mlp.ParseException;
import mlp.PomTaggedExpression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedList;

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
public class SemanticLatexTranslator extends AbstractTranslator implements IDLMFTranslator<PrintablePomTaggedExpression> {
    private static final Logger LOG = LogManager.getLogger(SemanticLatexTranslator.class.getName());

    /**
     * The latex parser
     */
    private SemanticMLPWrapper parser;

    private final ForwardTranslationProcessConfig config;

    private TranslatedExpression localTranslations;

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
        this.init();
    }

    /**
     * Returns the source language of this translator which is always "LaTeX"
     * @return the source language of this translator
     */
    @Override
    public String getSourceLanguage() {
        return config.getFROM_LANGUAGE();
    }

    /**
     * Returns the target language of this translator, i.e., the language to which the expressions will be translated to.
     * @return the translations the expressions will be translated to
     */
    @Override
    public String getTargetLanguage() {
        return config.getTO_LANGUAGE();
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

        return innerTranslate(expression, label, null);
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
            return new TranslationInformation();
        }

        innerTranslate(expression, label, null);
        return getTranslationInformationObject();
    }

    @Override
    public TranslationInformation translateToObjectFeatured(
            String expression,
            TranslationFeature<PrintablePomTaggedExpression> translationFeatures) {
        if ( expression == null || expression.isEmpty() ) {
            LOG.warn("Tried to translate an empty expression");
            return new TranslationInformation();
        }

        innerTranslate(expression, null, translationFeatures);
        return getTranslationInformationObject();
    }

    public TranslationInformation getTranslationInformationObject() {
        return super.getTranslationInformation();
    }

    private String innerTranslate( String expression, String label, TranslationFeature<PrintablePomTaggedExpression> translationFeatures ) throws TranslationException {
        try {
            PrintablePomTaggedExpression exp = parser.parse(expression, label);
            if ( translationFeatures != null ) {
                exp = translationFeatures.preProcess(exp);
            }
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
        // This is the actual translation expression. Every other translation method
        // is calling this method to translate something. Hence, we only need to prepare translations
        // in this method, i.e., resetting everything and start from scratch.

        // 1) prepare new run, with reset and setup translation information object
        reset();

        boolean forceFillUp = false;
        if ( expression instanceof PrintablePomTaggedExpression ) {
            // in this case, we can trigger +/- splitting
            PrintablePomTaggedExpression copy = new PrintablePomTaggedExpression((PrintablePomTaggedExpression) expression);
            if ( PomTaggedExpressionNormalizer.normalizePm(copy, true) ) {
                // there was a PM and it split so we should translate the first case
                TranslatedExpression firstCase = translateSingleExpression(copy, false);
                reset();
                // change original expression (expression) to first case
                copy = new PrintablePomTaggedExpression((PrintablePomTaggedExpression) expression);
                PomTaggedExpressionNormalizer.normalizePm(copy, false);
                TranslatedExpression secondCase = translateSingleExpression(copy, false);
                reset();
                addPartialTranslation(firstCase);
                addPartialTranslation(secondCase);
                forceFillUp = true;
            }

            getTranslationInformation().setExpression( ((PrintablePomTaggedExpression) expression).getTexString() );
        }

        return translateSingleExpression(expression, forceFillUp);
    }

    private TranslatedExpression translateSingleExpression(PomTaggedExpression pte, boolean forceFillUp) {
        localTranslations = new TranslatedExpression();
        TranslatedExpression global = super.getGlobalTranslationList();

        // 2) perform translations
        parseGeneralExpression(pte, new LinkedList<>());

        // 3) clean up
        localTranslations.clear();
        localTranslations.addTranslatedExpression(global);
        localTranslations.addRequiredPackages(global.getRequiredPackages());

        updateTranslationInformation(forceFillUp);

        // 4) return result
        return localTranslations;
    }

    private void updateTranslationInformation(boolean forceFillUp) {
        TranslationInformation ti = super.getTranslationInformation();
        if ( getListOfPartialTranslations().isEmpty() || forceFillUp ) {
            perform(
                    TranslatedExpression::appendRelationalComponent,
                    getGlobalTranslationList().getElementsAfterRelation().getTranslatedExpression().trim()
            );
        } else getGlobalTranslationList().clearRelationalComponents();

        ti.setTranslatedExpression( localTranslations.getTranslatedExpression() );
        ti.setInformation( getInfoLogger() );
        TranslatedExpressionHelper.addTranslatedExpressionInformation( getGlobalTranslationList(), ti );

        for ( TranslatedExpression te : getListOfPartialTranslations() ) {
            TranslationInformation t = new TranslationInformation();
            TranslatedExpressionHelper.addTranslatedExpressionInformation(te, t);
            t.setInformation(getInfoLogger());
            ti.addTranslations(t);
        }
    }

    @Override
    public InformationLogger getInfoLogger(){
        return super.getInfoLogger();
    }

    @Override
    public TranslationInformation getTranslationInformation(){
        return super.getTranslationInformation();
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
