package gov.nist.drmf.interpreter.cas.common;

import gov.nist.drmf.interpreter.cas.blueprints.BlueprintMaster;
import gov.nist.drmf.interpreter.cas.translation.SemanticLatexTranslator;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.TranslationProcessConfig;
import gov.nist.drmf.interpreter.mlp.extensions.MacrosLexicon;

import java.io.IOException;

/**
 * @author Andre Greiner-Petter
 */
public class ForwardTranslationProcessConfig extends TranslationProcessConfig {

    private String TAB = "";
    private String MULTIPLY = "*";

    private BlueprintMaster limitParser = null;

    private boolean alternativeMode = false;

    private boolean isInit = false;

    private boolean extensiveOutput = false;

    public ForwardTranslationProcessConfig(String to_language) {
        this(to_language, false);
    }

    public ForwardTranslationProcessConfig(String to_language, boolean alternativeMode) {
        super(Keys.KEY_LATEX, to_language);

        int length = to_language.length()+1 > "DLMF: ".length() ?
                (to_language.length()+2) : "DLMF: ".length();
        for ( int i = 0; i <= length; i++ )
            TAB += " ";

        this.alternativeMode = alternativeMode;
    }

    public void init() throws IOException {
        super.init();
        MacrosLexicon.init();
        MULTIPLY = super.getSymbolTranslator().translateFromMLPKey( Keys.MLP_KEY_MULTIPLICATION );
        this.isInit = true;
    }

    public void setShortOutput() {
        extensiveOutput = false;
    }

    public String getTAB() {
        return TAB;
    }

    public String getMULTIPLY() {
        return MULTIPLY;
    }

    public void setLimitParser(BlueprintMaster limitParser) {
        this.limitParser = limitParser;
    }

    public BlueprintMaster getLimitParser() {
        return limitParser;
    }

    public boolean isAlternativeMode() {
        return alternativeMode;
    }

    public boolean shortOutput() {
        return !extensiveOutput;
    }
}
