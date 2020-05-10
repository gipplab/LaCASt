package gov.nist.drmf.interpreter.cas.common;

import gov.nist.drmf.interpreter.cas.blueprints.BlueprintMaster;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.TranslationProcessConfig;
import gov.nist.drmf.interpreter.mlp.MacrosLexicon;

import java.io.IOException;

/**
 * This is the configuration for the forward translator. It defines to which language (CAS) the translations
 * should be performed. Besides, it allows to fine control several other settings.
 *
 * @author Andre Greiner-Petter
 */
public class ForwardTranslationProcessConfig extends TranslationProcessConfig {
    private String TAB = "";
    private String MULTIPLY = "*";

    private BlueprintMaster limitParser = null;
    private boolean extensiveOutput = false;

    private boolean inlinePackageMode = false;

    private boolean isInit = false;

    public ForwardTranslationProcessConfig(String to_language) {
        super(Keys.KEY_LATEX, to_language);

        int length = to_language.length()+1 > "DLMF: ".length() ?
                (to_language.length()+2) : "DLMF: ".length();
        for ( int i = 0; i <= length; i++ )
            TAB += " ";
    }

    public void init() throws IOException {
        super.init();
        MULTIPLY = super.getSymbolTranslator().translateFromMLPKey( Keys.MLP_KEY_MULTIPLICATION );
        this.isInit = true;
    }

    public void setLimitParser(BlueprintMaster limitParser) {
        this.limitParser = limitParser;
    }

    /**
     * TODO Experimental settings
     * @param shortenOutput shortens the logging on info level
     */
    public void shortenOutput(boolean shortenOutput) {
        extensiveOutput = !shortenOutput;
    }

    /**
     * Sets the mode to use package to an inline mode.
     * If the CAS supports inline packages, the translator attempts to return
     * the translation with the necessary packages within one line translation.
     *
     * If this mode is false (default), the necessary package information is only
     * given by an extra comment.
     * @param inlinePackageMode on/off the inline mode for loading packages
     */
    public void setInlinePackageMode(boolean inlinePackageMode) {
        this.inlinePackageMode = inlinePackageMode;
    }

    public BlueprintMaster getLimitParser() {
        return limitParser;
    }

    public boolean shortenedOutput() {
        return !extensiveOutput;
    }

    public boolean isInlinePackageMode() {
        return inlinePackageMode;
    }

    public String getTAB() {
        return TAB;
    }

    public String getMULTIPLY() {
        return MULTIPLY;
    }
}
