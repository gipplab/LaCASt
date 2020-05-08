package gov.nist.drmf.interpreter.cas.translation.components.util;

import gov.nist.drmf.interpreter.cas.common.ForwardTranslationProcessConfig;
import gov.nist.drmf.interpreter.cas.common.IForwardTranslator;
import gov.nist.drmf.interpreter.cas.logging.TranslatedExpression;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.interfaces.IPackageWrapper;
import gov.nist.drmf.interpreter.common.symbols.BasicFunctionsTranslator;
import gov.nist.drmf.interpreter.common.symbols.SymbolTranslator;

import java.util.Set;

/**
 * @author Andre Greiner-Petter
 */
public class PackageWrapper implements IPackageWrapper<TranslatedExpression, String> {
    private final BasicFunctionsTranslator functionsTranslator;
    private final SymbolTranslator symbolTranslator;

    public PackageWrapper(ForwardTranslationProcessConfig config) {
        this.functionsTranslator = config.getBasicFunctionsTranslator();
        this.symbolTranslator = config.getSymbolTranslator();
    }

    @Override
    public String addPackages(TranslatedExpression translatedExpression, Set<String> packages) {
        StringBuilder sb = new StringBuilder();
        addPackages(sb, packages, true);
        sb.append(translatedExpression.getTranslatedExpression());
        sb.append(symbolTranslator.translateFromMLPKey(Keys.MLP_KEY_END_OF_LINE));
        addPackages(sb, packages, false);
        return sb.toString();
    }

    private void addPackages(StringBuilder sb, Set<String> packages, boolean open) {
        String key = open ? Keys.MLP_KEY_LOAD_PACKAGE : Keys.MLP_KEY_UNLOAD_PACKAGE;
        for ( String p : packages ) {
            sb.append(functionsTranslator.translate(new String[]{p}, key));
            sb.append(symbolTranslator.translateFromMLPKey(Keys.MLP_KEY_SUPPRESS_OUTPUT));
        }
    }
}
