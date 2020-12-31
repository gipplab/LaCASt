package gov.nist.drmf.interpreter.generic.mlp.pojo;

import gov.nist.drmf.interpreter.cas.logging.TranslatedExpression;
import gov.nist.drmf.interpreter.cas.translation.SemanticLatexTranslator;
import gov.nist.drmf.interpreter.common.config.CASSupporter;
import gov.nist.drmf.interpreter.common.exceptions.InitTranslatorException;
import gov.nist.drmf.interpreter.common.interfaces.ITranslator;
import gov.nist.drmf.interpreter.pom.extensions.PrintablePomTaggedExpression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * @author Andre Greiner-Petter
 */
public class CASTranslators {
    private static final Logger LOG = LogManager.getLogger(CASTranslators.class.getName());

    private final Map<String, SemanticLatexTranslator> translatorMap;
    private final List<String> supportedCAS;

    public CASTranslators() {
        CASSupporter supporter = CASSupporter.getSupportedCAS();
        this.translatorMap = new HashMap<>();
        this.supportedCAS = new LinkedList<>();
        for ( String cas : supporter.getAllCAS() ) {
            try {
                SemanticLatexTranslator slt = new SemanticLatexTranslator(cas);
                translatorMap.put(cas, slt);
            } catch (InitTranslatorException e) {
                LOG.warn("Unable to setup semantic translator for language " + cas, e);
            }
        }
    }

    public CASTranslators(List<SemanticLatexTranslator> translators) {
        this.translatorMap = new HashMap<>();
        this.supportedCAS = new LinkedList<>();

        for (SemanticLatexTranslator translator : translators) {
            translatorMap.put( translator.getSourceLanguage(), translator );
            supportedCAS.add( translator.getSourceLanguage() );
        }
    }

    public String translate(String cas, String expr) {
        if ( !translatorMap.containsKey(cas) ) throw new IllegalArgumentException("Translations to given CAS " + cas + " unsupported.");
        SemanticLatexTranslator slt = translatorMap.get(cas);
        return slt.translate(expr);
    }

    public String translate(String cas, PrintablePomTaggedExpression expr) {
        if ( !translatorMap.containsKey(cas) ) throw new IllegalArgumentException("Translations to given CAS " + cas + " unsupported.");
        SemanticLatexTranslator slt = translatorMap.get(cas);
        TranslatedExpression te = slt.translate(expr);
        return te.getTranslatedExpression();
    }

    public Map<String, ITranslator> getTranslators() {
        return new HashMap<>(translatorMap);
    }

    public List<String> getSupportedCAS() {
        return supportedCAS;
    }
}
