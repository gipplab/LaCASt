package gov.nist.drmf.interpreter.cas.mlp;

import java.nio.file.Path;

/**
 * @author Andre Greiner-Petter
 */
public class CSVtoLexiconConverter {

    private Path csv_dir;

    public CSVtoLexiconConverter ( Path csv_dir ){
        this.csv_dir = csv_dir;
    }

    public void generateMacroLexicon( Path dlmf_macro_file ){

    }

    public void generateCASAddon( Path cas_translation_file ){

    }
}
