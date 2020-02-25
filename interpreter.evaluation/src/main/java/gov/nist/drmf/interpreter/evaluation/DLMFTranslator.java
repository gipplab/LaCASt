package gov.nist.drmf.interpreter.evaluation;

import gov.nist.drmf.interpreter.cas.translation.SemanticLatexTranslator;
import gov.nist.drmf.interpreter.common.constants.GlobalPaths;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.evaluation.constraints.IConstraintTranslator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

/**
 * @author Andre Greiner-Petter
 */
public class DLMFTranslator implements IConstraintTranslator {
    private static final Logger LOG = LogManager.getLogger(DLMFTranslator.class.getName());

    private final SemanticLatexTranslator dlmfInterface;

    public DLMFTranslator( String cas ) throws IOException {
        dlmfInterface = new SemanticLatexTranslator( cas );
        dlmfInterface.init( GlobalPaths.PATH_REFERENCE_DATA );
        LOG.debug("Initialized DLMF LaTeX Interface.");
    }

    @Override
    public String translate(String expression, String label) throws TranslationException {
        return dlmfInterface.translate(expression, label);
    }
}
