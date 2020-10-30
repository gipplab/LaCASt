package gov.nist.drmf.interpreter.pom.data;

import gov.nist.drmf.interpreter.common.constants.GlobalPaths;

import java.nio.file.Path;

/**
 * @author Andre Greiner-Petter
 */
public class LexiconConverterConfig {

    private Path csvPath;
    private Path dlmfMacroLexiconPath;

    public LexiconConverterConfig() {
        csvPath = GlobalPaths.PATH_REFERENCE_DATA_CSV;
        dlmfMacroLexiconPath = GlobalPaths.DLMF_MACROS_LEXICON;
    }

    public LexiconConverterConfig(Path csvDirPath, Path dlmfMacroLexPath) {
        this.csvPath = csvDirPath;
        this.dlmfMacroLexiconPath = dlmfMacroLexPath;
    }

    public Path getCsvPath() {
        return csvPath;
    }

    public Path getDlmfMacroLexiconPath() {
        return dlmfMacroLexiconPath;
    }
}
