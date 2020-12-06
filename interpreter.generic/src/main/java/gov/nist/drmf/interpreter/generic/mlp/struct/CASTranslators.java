package gov.nist.drmf.interpreter.generic.mlp.struct;

import gov.nist.drmf.interpreter.cas.logging.TranslatedExpression;
import gov.nist.drmf.interpreter.cas.translation.SemanticLatexTranslator;
import gov.nist.drmf.interpreter.common.config.CASSupporter;
import gov.nist.drmf.interpreter.common.exceptions.InitTranslatorException;
import gov.nist.drmf.interpreter.pom.extensions.PrintablePomTaggedExpression;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Andre Greiner-Petter
 */
public class CASTranslators {

    private static CASTranslators translators;

    private final Map<String, SemanticLatexTranslator> translatorMap;
    private final List<String> supportedCAS;

    private CASTranslators() throws InitTranslatorException {
        CASSupporter supporter = CASSupporter.getSupportedCAS();
        this.translatorMap = new HashMap<>();
        this.supportedCAS = supporter.getAllCAS();
        for ( String cas : supportedCAS ) {
            SemanticLatexTranslator slt = new SemanticLatexTranslator(cas);
            translatorMap.put(cas, slt);
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

    public List<String> getSupportedCAS() {
        return supportedCAS;
    }

    public static CASTranslators getTranslatorsInstance() throws InitTranslatorException {
        if ( translators == null ) {
            translators = new CASTranslators();
        }
        return translators;
    }

}
