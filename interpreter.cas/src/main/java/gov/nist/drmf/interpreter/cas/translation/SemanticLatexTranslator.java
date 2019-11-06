package gov.nist.drmf.interpreter.cas.translation;

import gov.nist.drmf.interpreter.cas.blueprints.BlueprintMaster;
import gov.nist.drmf.interpreter.cas.common.ForwardTranslationProcessConfig;
import gov.nist.drmf.interpreter.cas.logging.TranslatedExpression;
import gov.nist.drmf.interpreter.common.*;
import gov.nist.drmf.interpreter.common.constants.GlobalPaths;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.mlp.extensions.MacrosLexicon;
import mlp.ParseException;
import mlp.PomParser;
import mlp.PomTaggedExpression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;

/**
 * This translation translate semantic LaTeX formula using
 * the math processor language by Abdou Youssef.
 * It based on BNF grammar programmed with JavaCC.
 *
 * It is the top level translation objects. That means
 * you can use {@link #translate(String)} to translate an
 * expression in general. To do so, you have to
 * invoke {@link #init(Path)} before you use this
 * translate method. On the other hand this translation can
 * handle also general PomTaggedExpression to translate.
 * @see PomTaggedExpression
 *
 * @author Andre Greiner-Petter
 */
public class SemanticLatexTranslator extends AbstractTranslator {
    private static final Logger LOG = LogManager.getLogger(SemanticLatexTranslator.class.getName());

    /**
     * The latex parser
     */
    private PomParser parser;

    private ForwardTranslationProcessConfig config;

    private TranslatedExpression localTranslations;

    /**
     * Creates a forward translator to the specified language.
     * @param to_language the language key
     * @see gov.nist.drmf.interpreter.common.constants.Keys
     */
    public SemanticLatexTranslator( String to_language ){
        this(new ForwardTranslationProcessConfig(to_language));
    }

    /**
     * Creates a forward translator based on a the given config.
     * If the config does not contain a {@link BlueprintMaster} object
     * yet, it will be initialized in the {@link #init(Path)} function
     * and added to the config object.
     *
     * @param config config (with or without BlueprintMaster object)
     * @see BlueprintMaster
     */
    public SemanticLatexTranslator( ForwardTranslationProcessConfig config ) {
        super(null);
        super.setConfig(config);
        this.config = config;
        this.localTranslations = new TranslatedExpression();
    }

    /**
     * Copy constructor
     * @param orig the original translator
     */
    private SemanticLatexTranslator( SemanticLatexTranslator orig ) {
        super(null);
        super.setConfig(orig.getConfig());
        this.config = orig.config;
        this.parser = orig.parser;
        this.localTranslations = new TranslatedExpression();
    }

    @Nullable
    @Override
    public TranslatedExpression getTranslatedExpressionObject() {
        return localTranslations;
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
     * @param reference_dir_path the path to the ReferenceData directory.
     *                           You can find the path in
     *                           {@link GlobalPaths#PATH_REFERENCE_DATA}.
     * @throws IOException if it is not possible to read the information
     *                      from the files.
     */
    public void init( Path reference_dir_path ) throws IOException {
        config.init();

        parser = new PomParser(reference_dir_path.toString());
        parser.addLexicons( MacrosLexicon.getDLMFMacroLexicon() );

        if ( config.getLimitParser() == null ) {
            BlueprintMaster bm = new BlueprintMaster(
                    new SemanticLatexTranslator(this) // copy
            );
            bm.init();
            config.setLimitParser(bm);
        }
    }

    /**
     * Translates a given string to the another language.
     * @param expression the expression that should get translated.
     * @return the translated expression.
     * @throws TranslationException if an error occurred due translation
     */
    public String translate( String expression ) throws TranslationException {
        if ( expression == null || expression.isEmpty() ) {
            LOG.warn("Tried to translate an empty expression");
            return "";
        }

        expression = TeXPreProcessor.preProcessingTeX(expression);
        LOG.trace("Preprocessed input string. Parsing: " + expression);

        try {
            PomTaggedExpression exp = parser.parse(expression);
            if (translate( exp )) {
                LOG.debug("Successfully translated " + expression);
                if ( !super.getInfoLogger().isEmpty() ) LOG.info(super.getInfoLogger().toString());
                return super.getGlobalTranslationList().getTranslatedExpression();
            } else {
                handleNull(
                        null,
                        "Wasn't able to translate the given expression.",
                        TranslationException.Reason.NULL,
                        expression,
                        null);
            }
        } catch ( ParseException pe ){
            handleNull(
                    null,
                    pe.getMessage(),
                    TranslationException.Reason.MLP_ERROR,
                    expression,
                    pe
            );
        }
        // this cannot happen -> a translation exception will be thrown
        return null;
    }

    @Override
    public boolean translate( PomTaggedExpression expression ) throws TranslationException {
        reset();
        localTranslations = new TranslatedExpression();
        TranslatedExpression global = super.getGlobalTranslationList();

        parseGeneralExpression(expression, null);

        localTranslations.clear();
        localTranslations.addTranslatedExpression(
                global
        );

        return !isInnerError();
    }

    @Override
    public InformationLogger getInfoLogger(){
        return super.getInfoLogger();
    }

    public BlueprintMaster getBlueprintMaster() {
        return config.getLimitParser();
    }
}
