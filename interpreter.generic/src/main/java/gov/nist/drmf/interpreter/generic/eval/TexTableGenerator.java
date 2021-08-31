package gov.nist.drmf.interpreter.generic.eval;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;

/**
 * @author Andre Greiner-Petter
 */
public class TexTableGenerator {
    private static final Logger LOG = LogManager.getLogger(TexTableGenerator.class.getName());

    private static final String TABLE_PRE_PROC =
            "\\begin{table}\n" +
            "\t\\centering\n" +
            "\t\\renewcommand{\\arraystretch}{1.4}\n" +
            "\t\\setlength{\\tabcolsep}{3pt}\n" +
            "\t\\begin{tabular}{r;{1pt/1pt}c;{1pt/1pt}c;{1pt/1pt}c;{1pt/1pt}c;{1pt/1pt}c;{1pt/1pt}c}\n" +
            "\t\\hline\\hline\n";

    private static final String TABLE_POST_PROC =
            "\t\\end{tabular}\n" +
            "\\end{table}\n";

    /**
     * %s: ID name
     * %s: ID name again
     * %s: TeX String
     */
    private static final String VERB =
            "\\newsavebox\\%s\n" +
            "\\begin{lrbox}{\\%s}\n" +
            " \\begin{minipage}[t]{0.82\\textwidth}\n" +
            "  \\lstinline[language={[latex]TeX},mathescape,breaklines=true]\"%s\"\n" +
            " \\end{minipage}\n" +
            "\\end{lrbox}\n";

    /**
     * %s: Original TeX Formula
     * %d: Test ID (numeral 1-95)
     * %s: URL (# escaped with \\#)
     * %s: URL Name
     * %s: LaTeX Original Verbatim ID
     * %s: Semantic LaTeX Verb ID
     * %s: Mathematica Verb ID
     * %s: Maple Verb ID
     */
    private static final String TABLE_ENTRY =
            "Formula & \\multicolumn{5}{Sl}{$%s$} &  \\multicolumn{1}{r}{\\textbf{%d:} \\href{%s}{%s}} \\\\\\hdashline[1pt/1pt]\n" +
            "\\LaTeX & \\multicolumn{6}{l}{\\usebox\\%s} \\\\\\hdashline[1pt/1pt]\n" +
            "\\makecell[tr]{Semantic\\\\ \\LaTeX} & \\multicolumn{6}{l}{\\usebox\\%s} \\\\\\hdashline[1pt/1pt]\n" +
            "Mathematica & \\multicolumn{6}{l}{\\usebox\\%s} \\\\\\hdashline[1pt/1pt]\n" +
            "Maple & \\multicolumn{6}{l}{\\usebox\\%s} \\\\\n" +
            "\\hline\n" +
            "\\multicolumn{2}{c;{1pt/1pt}}{\\textbf{Translations}} & \\multicolumn{5}{c}{\\textbf{Reason for Failure}} \\\\\\hline\n" +
            "\\makecell[bc]{\\textbf{Semantic}\\\\ \\LaTeX} & \\textbf{CAS} & \\makecell[bc]{\\textbf{Definition /}\\\\ \\textbf{Substitution}} & \\makecell[bc]{\\textbf{Pattern}\\\\ \\textbf{Matching}} & \\makecell[bc]{\\textbf{Derivatives /}\\\\ \\textbf{Primes}} & \\makecell[bc]{\\textbf{Missing}\\\\ \\textbf{Information}} & \\textbf{Untranslatable} \\\\\\hdashline[1pt/1pt]\n" +
            "\\multicolumn{1}{c;{1pt/1pt}}{{\\cellcolor{OliveGreen!20!white}} \\correct} & {\\cellcolor{Red!20!white}} \\wrong & - & - & - & - & - \\\\\\hline\n" +
            "\\textbf{Explanation} & \\multicolumn{6}{l}{ --- } \\\\\\hline\n" +
            "\\hline\n";

    private final Path outpath;

    private StringBuilder verbWriter, tableWriter;

    private LinkedList<String> finishedTables;
    private int innerCounter = 0;

    public TexTableGenerator(Path outputPath) {
        outpath = outputPath;
        verbWriter = new StringBuilder();
        tableWriter = new StringBuilder(TABLE_PRE_PROC);
        innerCounter = 0;
        finishedTables = new LinkedList<>();
    }

    public void addEntry(TexTableEntry entry) {
        // verb defs
        String verbTex = buildVerb(entry.buildID("T"), entry.getTex());
        String verbSTex = buildVerb(entry.buildID("ST"), entry.getSemantictex());
        String verbMath = buildVerb(entry.buildID("MM"), entry.getMathematica());
        String verbMap = buildVerb(entry.buildID("MA"), entry.getMaple());

        verbWriter.append("%%%%%%%%%%%%%%%%%%");
        verbWriter.append(entry.getNum()).append(": ").append(entry.getUrlName());
        verbWriter.append("%%%%%%%%%%%%%%%%%%\n");
        verbWriter.append(verbTex);
        verbWriter.append(verbSTex);
        verbWriter.append(verbMath);
        verbWriter.append(verbMap);

        if ( (innerCounter) % 3 == 0 && innerCounter > 0 ) {
            tableWriter.append(TABLE_POST_PROC);
            finishedTables.addLast( tableWriter.toString() );
            tableWriter = new StringBuilder(TABLE_PRE_PROC);
        }

        String tex = "";//entry.getTex().length() > 100 ? "\\scriptsize " : "";
        tex += entry.getTex();
        tex = tex.replace("{align}", "{aligned}");

        String tableEntry = String.format(TABLE_ENTRY,
                tex,
                entry.getNum(),
                entry.getUrl(),
                entry.getUrlName(),
                entry.buildID("T"),
                entry.buildID("ST"),
                entry.buildID("MM"),
                entry.buildID("MA")
        );

        tableWriter.append(tableEntry);
        innerCounter++;
    }

    public void flush() {
        tableWriter.append(TABLE_POST_PROC);
        finishedTables.addLast(tableWriter.toString());
    }

    public void write() throws IOException {
        String verbDefPathFileName = "formula-verb-defs.tex";
        Files.writeString( outpath.resolve(verbDefPathFileName), verbWriter.toString() );

        String tablesContentFile = "\\input{./" + verbDefPathFileName + "}\n";
        tablesContentFile += String.join( "\n", finishedTables );

        Files.writeString( outpath.resolve("tables.tex"), tablesContentFile );
        LOG.info("Finished writing files in " + outpath);
    }

    private String buildVerb(String id, String cont) {
        return String.format(VERB, id, id, cont);
    }
}
