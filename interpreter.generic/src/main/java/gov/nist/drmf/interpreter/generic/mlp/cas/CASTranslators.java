package gov.nist.drmf.interpreter.generic.mlp.cas;

import gov.nist.drmf.interpreter.common.config.CASSupporter;
import gov.nist.drmf.interpreter.common.exceptions.InitTranslatorException;
import gov.nist.drmf.interpreter.common.interfaces.IConstraintTranslator;
import gov.nist.drmf.interpreter.core.api.DLMFTranslator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Andre Greiner-Petter
 */
public class CASTranslators {
    private static final Logger LOG = LogManager.getLogger(CASTranslators.class.getName());

    private final Map<String, DLMFTranslator> translatorMap;

    public CASTranslators() {
        CASSupporter supporter = CASSupporter.getSupportedCAS();
        this.translatorMap = new HashMap<>();
        for ( String cas : supporter.getAllCAS() ) {
            try {
                DLMFTranslator slt = new DLMFTranslator(cas);
                translatorMap.put(cas, slt);
            } catch (InitTranslatorException e) {
                LOG.warn("Unable to setup semantic translator for language " + cas, e);
            }
        }
    }

    public IConstraintTranslator getTranslator(String cas) {
        return translatorMap.get(cas);
    }

    public Map<String, IConstraintTranslator> getTranslators() {
        return new HashMap<>(translatorMap);
    }
}
