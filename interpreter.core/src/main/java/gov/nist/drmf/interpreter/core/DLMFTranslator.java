package gov.nist.drmf.interpreter.core;

import gov.nist.drmf.interpreter.cas.constraints.IConstraintTranslator;
import gov.nist.drmf.interpreter.cas.translation.SemanticLatexTranslator;
import gov.nist.drmf.interpreter.common.TranslationProcessConfig;
import gov.nist.drmf.interpreter.common.cas.PackageWrapper;
import gov.nist.drmf.interpreter.common.constants.GlobalPaths;
import gov.nist.drmf.interpreter.common.exceptions.InitTranslatorException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.interfaces.IPackageWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Set;

/**
 * @author Andre Greiner-Petter
 */
public class DLMFTranslator implements IConstraintTranslator {
    private static final Logger LOG = LogManager.getLogger(DLMFTranslator.class.getName());

    private final SemanticLatexTranslator dlmfInterface;
    private final TranslationProcessConfig config;
    private final PackageWrapper packageWrapper;

    public DLMFTranslator( String cas ) throws InitTranslatorException {
        dlmfInterface = new SemanticLatexTranslator( cas );
        config = dlmfInterface.getConfig();
        packageWrapper = new PackageWrapper(config);
        LOG.debug("Initialized DLMF LaTeX Interface.");
    }

    @Override
    public String translate(String expression, String label) throws TranslationException {
        return dlmfInterface.translate(expression, label);
    }

    @Override
    public Set<String> getRequiredPackages() {
        return dlmfInterface.getTranslatedExpressionObject().getRequiredPackages();
    }

    @Override
    public IPackageWrapper<String, String> getPackageWrapper() {
        return packageWrapper;
    }
}
