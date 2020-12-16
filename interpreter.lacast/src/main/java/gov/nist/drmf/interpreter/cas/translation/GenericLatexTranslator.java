package gov.nist.drmf.interpreter.cas.translation;

import gov.nist.drmf.interpreter.cas.common.ForwardTranslationProcessConfig;
import gov.nist.drmf.interpreter.common.exceptions.InitTranslatorException;

/**
 * The extension of the semantic LaTeX translator allows to translate non-semantic LaTeX expressions to CAS by
 * providing a textual context.
 *
 * @author Andre Greiner-Petter
 */
public class GenericLatexTranslator extends SemanticLatexTranslator {
    public GenericLatexTranslator(String to_language) throws InitTranslatorException {
        super(to_language);
    }

    public GenericLatexTranslator(ForwardTranslationProcessConfig config) throws InitTranslatorException {
        super(config);
    }
}
