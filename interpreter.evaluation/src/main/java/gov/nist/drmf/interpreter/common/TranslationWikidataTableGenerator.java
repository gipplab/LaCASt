package gov.nist.drmf.interpreter.common;

import gov.nist.drmf.interpreter.cas.translation.SemanticLatexTranslator;
import gov.nist.drmf.interpreter.common.constants.GlobalPaths;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.evaluation.diff.NumericalDifferencesAnalyzer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Andre Greiner-Petter
 */
public class TranslationWikidataTableGenerator {
    private static final Logger LOG = LogManager.getLogger(TranslationWikidataTableGenerator.class.getName());

    private static final Pattern QID_PATTERN = Pattern.compile("(Q\\d+),DLMF:(.*?),.*");

    private static final Pattern SYMBOLIC_PATTERN = Pattern.compile(
            "^(\\d+(?:-[a-z])?)(?: \\[.*?])?: (Successful|Error|Skipped|Failure).*$"
    );

    private static final Pattern NUMERIC_PATTERN = Pattern.compile(
            "^(\\d+(?:-[a-z])?): (Skip|Successful|Error|\\$Failed|[\\[{]{2}.*[]}]{3}).*$"
    );

    private String COLLAPSE_ELEMENT = "<div class=\"toccolours mw-collapsible mw-collapsed\">%s<div class=\"mw-collapsible-content\">%s</div></div>";

    private String TABLE_LINE =
            "| [[Item:%s|%s]] || <math>%s</math> || <code>%s</code> || <code>%s</code> || " +
                    "%s || %s || " +
                    "%s || %s " +
                    "\n";

    private SymbolDefinedLibrary symbolDefinitionLibrary;

    private Path dataset, symbolicMaple, numericMaple, symbolicMath, numericMath, qidmapping;

    private HashMap<String, String> qidLib;

    private HashMap<String, String> mapleSym, mapleNum, mathSym, mathNum;

    private SemanticLatexTranslator mapleTranslator, mathematicaTranslator;

    public TranslationWikidataTableGenerator(
            Path datasetPath,
            Path symbolicResultsMaple,
            Path numericResultsMaple,
            Path symbolicResultsMath,
            Path numericResultsMath,
            Path qidMappingPath
    ) {
        this.dataset = datasetPath;
        this.symbolicMaple = symbolicResultsMaple;
        this.numericMaple = numericResultsMaple;
        this.symbolicMath = symbolicResultsMath;
        this.numericMath = numericResultsMath;
        this.qidmapping = qidMappingPath;

        this.symbolDefinitionLibrary = new SymbolDefinedLibrary();

        this.qidLib = new HashMap<>();
        this.mapleTranslator = new SemanticLatexTranslator(Keys.KEY_MAPLE);
        this.mathematicaTranslator = new SemanticLatexTranslator(Keys.KEY_MATHEMATICA);

        this.mapleSym = new HashMap<>();
        this.mapleNum = new HashMap<>();
        this.mathSym = new HashMap<>();
        this.mathNum = new HashMap<>();
    }

    public void init() throws IOException {
        LOG.info("Init Maple translator");
        this.mapleTranslator.init(GlobalPaths.PATH_REFERENCE_DATA);

        LOG.info("Init Mathematica translator");
        this.mathematicaTranslator.init(GlobalPaths.PATH_REFERENCE_DATA);

        LOG.info("Init QID - DLMF library");
        loadQIDLib();

        LOG.info("Load symbolic test results of Maple");
        Files.walk(symbolicMaple)
                .filter( f -> Files.isRegularFile(f) )
                .filter( f -> f.toString().contains("symbolic"))
                .forEach( p -> loadResults(p, SYMBOLIC_PATTERN, mapleSym));

        LOG.info("Load symbolic test results of Mathematica");
        Files.walk(symbolicMath)
                .filter( f -> Files.isRegularFile(f) )
                .filter( f -> f.toString().contains("symbolic"))
                .forEach( p -> loadResults(p, SYMBOLIC_PATTERN, mathSym));

        LOG.info("Load numeric test results of Maple");
        Files.walk(numericMaple)
                .filter( f -> Files.isRegularFile(f) )
                .forEach( p -> loadResults(p, NUMERIC_PATTERN, mapleNum));

        LOG.info("Load numeric test results of Mathematica");
        Files.walk(numericMath)
                .filter( f -> Files.isRegularFile(f) )
                .forEach( p -> loadResults(p, NUMERIC_PATTERN, mathNum));
    }

    private void loadResults(Path path, Pattern pattern, HashMap<String, String> lib) {
        try {
            Files.lines(path)
                    .skip(1)
                    .forEach(l -> {
                        Matcher m = pattern.matcher(l);
                        if ( m.matches() ) {
                            String lineStr = m.group(1);
                            String res = m.group(2);
                            lib.put(lineStr, res);
                        }
                    });
        } catch ( IOException ioe ) {
            LOG.error("Cannot load results from " + path, ioe);
        }
    }

    private void loadQIDLib() throws IOException {
        Files.lines(qidmapping)
                .forEach( l -> {
                    Matcher m = QID_PATTERN.matcher(l);
                    if ( m.matches() ) {
                        qidLib.put(m.group(2), m.group(1));
                    }
                });
    }

    public void generateTable( Path outputFile ) throws IOException {
        int[] line = new int[]{0};
        LinkedList<LinkedList<Case>> allCases = new LinkedList<>();
        LOG.info("Load dataset.");
        Files.lines(dataset)
                .sequential()
                .peek(l -> line[0]++)
                .forEach(l -> {
                    LinkedList<Case> cases = CaseAnalyzer.analyzeLine(l, line[0], symbolDefinitionLibrary);
                    if ( cases != null && !cases.isEmpty() ) allCases.add(cases);
                });

        LOG.info("Start analyzing the data...");
        try ( BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile.toFile())) ) {
            writer.write("{| class=\"wikitable sortable\"\n|-\n");
            writer.write("! DLMF !! Formula !! Maple !! Mathematica !! Symbolic<br>Maple !! Symbolic<br>Mathematica !! Numeric<br>Maple !! Numeric<br>Mathematica\n|-\n");

//            int securityCounter = 0;
            for ( LinkedList<Case> lineCase : allCases ) {
//                if ( securityCounter > 50 ) break;
//                securityCounter++;

                Case c = lineCase.get(0);
                String id = ""+c.getLine();
                singleCase(id, c, writer);

                char a = 'a';
                for ( int i = 1; i < lineCase.size(); i++ ) {
                    c = lineCase.get(i);
                    id = c.getLine() + "-" + a;
                    singleCase(id, c, writer);
                    a++;
                }
            }

            writer.write("|}");
        } catch ( IOException ioe ) {
            LOG.error("Unable to write file.", ioe);
        }
    }

    private void singleCase( String id, Case c, BufferedWriter writer ) throws IOException {
        LOG.info("Writing " + id);
        String expr = c.getLHS() + " " + c.getRelation().getSymbol() + " " + c.getRHS();
        String label = c.getEquationLabel();
        String maple = "", mathematica = "";
        String qid = qidLib.get(c.getEquationLabel());
        String symbMaple = mapleSym.get(id);
        String symbMath = mathSym.get(id);

        if ( symbMaple == null && symbMath == null ) return;

        try {
            maple = mapleTranslator.translate(expr, label);
        } catch ( Exception e ){
            maple = "Error";
        }

        try {
            mathematica = mathematicaTranslator.translate(expr, label);
        } catch ( Exception e ){
            mathematica = "Error";
        }

        if ( maple.equals("Error") && mathematica.equals("Error") ) return;

        if ( symbMaple == null ) symbMaple = "Error";
        if ( symbMath == null ) symbMath = "Error";
        String numericMaple = symbMaple.equals("Failure") ? getNumericResultString( true, id ) : "-";
        String numericMath = symbMath.equals("Failure") ? getNumericResultString( false, id ) : "-";

        String line = String.format(TABLE_LINE, qid, label, expr, maple, mathematica, symbMaple, symbMath, numericMaple, numericMath);

        LOG.info(line);
        writer.write(line);
        writer.write("|-\n");
    }

    private String getNumericResultString( boolean mapleMode, String id ) {
        Pattern elPattern = mapleMode ?
                NumericalDifferencesAnalyzer.mapleEntityPattern :
                NumericalDifferencesAnalyzer.mathEntityPattern;

        String result = mapleMode ? mapleNum.get(id) : mathNum.get(id);
        if ( result == null ) return "-";

        if ( result.startsWith("[") || result.startsWith("{") ) {
            if ( result.contains("Error") ) return "Error";

            Matcher m = elPattern.matcher(result);
            StringBuilder list = new StringBuilder();
            while ( m.find() ) {
                list.append("<code>").append(m.group(1)).append(" <- {").append(m.group(2)).append("}</code><br>");
            }
            return String.format(COLLAPSE_ELEMENT, "Fail", list.toString());
        } else {
            if ( result.contains("Failed") ) return "Error";
            else return result;
        }
    }

    public static void main(String[] args) throws IOException {
        TranslationWikidataTableGenerator t = new TranslationWikidataTableGenerator(
                Paths.get("/home/andreg-p/Howard/together.txt"),
                Paths.get("misc/Results/MapleSymbolic"),
                Paths.get("misc/Results/MapleNumeric"),
                Paths.get("misc/Results/MathematicaSymbolic"),
                Paths.get("misc/Results/MathematicaNumeric"),
                Paths.get("/home/andreg-p/Howard/formulaQ.csv")
        );

        t.init();
        t.generateTable(Paths.get("misc/result-table.txt"));
    }
}
