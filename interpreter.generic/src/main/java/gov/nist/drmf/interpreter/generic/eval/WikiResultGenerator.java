package gov.nist.drmf.interpreter.generic.eval;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * @author Andre Greiner-Petter
 */
public class WikiResultGenerator {
    private static final Logger LOG = LogManager.getLogger(WikiResultGenerator.class.getName());

    private static final String CORRECT = "{{ya}}";
    private static final String WRONG = "{{na}}";

    /**
     * %s: Title
     * %d: ID
     * %s: URL
     * %s %s: Tex twice
     *
     * %s: Correct Semantic LaTex
     * %s: Correct Mathematica
     * %s: Correct Maple
     *
     * %s: Semantic Latex Translation
     * %s: Semantic Latex Goldi
     *
     * %s: Mathematica Translation
     * %s: Mathematica Translation
     *
     * %s: Maple Translation
     * %s: Maple Translation
     */
    private static final String PAGE =
            "__NOTOC__\n" +
            "== %s ==\n" +
            "\n" +
            "; Gold ID : %d\n" +
            "; Link : %s\n" +
            "; Formula : <math>%s</math>\n" +
            "; TeX Source : <syntaxhighlight lang=\"tex\" inline>%s</syntaxhighlight>\n" +
            "\n" +
            "{| class=\"wikitable\"\n" +
            "|-\n" +
            "! colspan=\"3\" | Translation Results\n" +
            "|-\n" +
            "! Semantic LaTeX !! Mathematica Translation !! Maple Translations\n" +
            "|-\n" +
            "| %s\n" +
            "| %s\n" +
            "| %s\n" +
            "|}\n" +
            "\n" +
            "=== Semantic LaTeX ===\n" +
            "\n" +
            "; Translation : <syntaxhighlight lang=\"tex\" inline>%s</syntaxhighlight>\n" +
            "; Expected (Gold Entry) : <syntaxhighlight lang=\"tex\" inline>%s</syntaxhighlight>\n" +
            "\n" +
            "\n" +
            "=== Mathematica ===\n" +
            "\n" +
            "; Translation : <syntaxhighlight lang=\"mathematica\" inline>%s</syntaxhighlight>\n" +
            "; Expected (Gold Entry) : <syntaxhighlight lang=\"mathematica\" inline>%s</syntaxhighlight>\n" +
            "\n" +
            "\n" +
            "=== Maple ===\n" +
            "\n" +
            "; Translation : <syntaxhighlight lang=\"mathematica\" inline>%s</syntaxhighlight>\n" +
            "; Expected (Gold Entry) : <syntaxhighlight lang=\"mathematica\" inline>%s</syntaxhighlight>\n";

    private final Path outPath;

    public WikiResultGenerator(Path out) {
        this.outPath = out;
    }

    public void addResult(TexTableEntry entry) throws IOException {
        String tmm = entry.isCorrectTMM() ?
                CORRECT :
                (entry.getMathematica().isBlank() ? "-" : WRONG);
        String tma = entry.isCorrectTMA() ?
                CORRECT :
                (entry.getMaple().isBlank() ? "-" : WRONG);

        String page = String.format(PAGE,
                entry.getTitle(),
                entry.getNum(),
                entry.getUrl().replace("\\", ""),
                entry.getTex(), entry.getTex(),
                entry.isCorrectTST() ? CORRECT : WRONG,
                tmm,
                tma,
                entry.getTranslationST(),
                entry.getSemantictex(),
                entry.getTranslationMM(),
                entry.getMathematica(),
                entry.getTranslationMA(),
                entry.getMaple()
        );

        Path file = outPath.resolve( entry.getNum()+".txt" );
//        LOG.info("Writing result " + entry.getNum());
        Files.writeString(file, page);
    }
}
