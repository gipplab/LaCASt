package gov.nist.drmf.interpreter.common.cas;

import gov.nist.drmf.interpreter.common.TranslationProcessConfig;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.interfaces.IPackageWrapper;
import gov.nist.drmf.interpreter.common.symbols.BasicFunctionsTranslator;
import gov.nist.drmf.interpreter.common.symbols.SymbolTranslator;

import java.util.Set;

/**
 * @author Andre Greiner-Petter
 */
public class PackageWrapper implements IPackageWrapper<String, String> {
    private final BasicFunctionsTranslator functionsTranslator;
    private final SymbolTranslator symbolTranslator;

    private final String endOfLineSymbol;

    public PackageWrapper(TranslationProcessConfig config) {
        this(config.getBasicFunctionsTranslator(), config.getSymbolTranslator());
    }

    public PackageWrapper(BasicFunctionsTranslator functionsTranslator, SymbolTranslator symbolTranslator) {
        this.functionsTranslator = functionsTranslator;
        this.symbolTranslator = symbolTranslator;
        this.endOfLineSymbol = symbolTranslator.translateFromMLPKey(Keys.MLP_KEY_END_OF_LINE);
    }

    @Override
    public String addPackages(String translatedExpression, Set<String> packages) {
        if ( packages == null || packages.isEmpty() ) return translatedExpression;
        StringBuilder sb = new StringBuilder();
        addPackages(sb, packages, true);
        sb.append(" "); // slightly improves readability
        sb.append(translatedExpression);
        if ( !translatedExpression.endsWith(endOfLineSymbol) )
            sb.append(endOfLineSymbol);
        sb.append(" ");
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
