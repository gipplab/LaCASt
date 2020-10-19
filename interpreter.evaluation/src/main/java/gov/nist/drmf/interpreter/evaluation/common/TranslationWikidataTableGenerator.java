package gov.nist.drmf.interpreter.evaluation.common;

import gov.nist.drmf.interpreter.cas.translation.SemanticLatexTranslator;
import gov.nist.drmf.interpreter.common.constants.GlobalPaths;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.exceptions.InitTranslatorException;
import gov.nist.drmf.interpreter.core.DLMFTranslator;
import gov.nist.drmf.interpreter.evaluation.core.AbstractEvaluator;
import gov.nist.drmf.interpreter.evaluation.core.EvaluationConfig;
import gov.nist.drmf.interpreter.evaluation.core.diff.NumericalDifferencesAnalyzer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Andre Greiner-Petter
 */
public class TranslationWikidataTableGenerator extends AbstractEvaluator<String> {
    private static final Logger LOG = LogManager.getLogger(TranslationWikidataTableGenerator.class.getName());

    private static final int LIMIT_ENTRIES = 4;

    private static final int CHAR_LIMIT = 1_000;

    private static final Pattern QID_PATTERN = Pattern.compile("(Q\\d+),DLMF:(.*?),.*");

    private static final Pattern FALL_BACK_PATTERN = Pattern.compile(
            "^(\\d+(?:-[a-z])?)(?: \\[.*?])?: (.*)"
    );

    private static final Pattern SYMBOLIC_PATTERN = Pattern.compile(
            "^(\\d+(?:-[a-z])?)(?: \\[.*?])?: (?:All )?(Successful|Error|Skipped.*|Failure|Aborted|Missing Macro Error|Definition|Translation Error).*$"
    );

    private static final Pattern NUMERIC_PATTERN = Pattern.compile(
            "^(\\d+(?:-[a-z])?)(?: \\[.*?])?: (Skip.*|Successful.*|Error.*|Failed.*|Aborted|Manual Skip)$"
    );

    private String COLLAPSE_ELEMENT = "<div class=\"toccolours mw-collapsible mw-collapsed\">%s<div class=\"mw-collapsible-content\">%s</div></div>";

    private String TABLE_LINE =
            "| [https://dlmf.nist.gov/%s %s] || [[Item:%s|<math>%s</math>]] || <code>%s</code> || <code>%s</code> || " +
                    "%s || %s || " +
                    "%s || %s\n";

    private SymbolDefinedLibrary symbolDefinitionLibrary;

    private Path dataset, symbolicMaple, numericMaple, symbolicMath, numericMath, numericMathSymbSuc, qidmapping;

    private HashMap<String, String> qidLib;

    private HashMap<String, String> mapleSym, mapleNum, mathSym, mathNum;

    private SemanticLatexTranslator mapleTranslator, mathematicaTranslator;

    private int[] range = new int[]{0,1};

    public TranslationWikidataTableGenerator(
            Path datasetPath,
            Path symbolicResultsMaple,
            Path numericResultsMaple,
            Path symbolicResultsMath,
            Path numericResultsMath,
            Path numericSymbSuccResultsMath,
            Path qidMappingPath
    ) throws InitTranslatorException {
        super(new DLMFTranslator(Keys.KEY_MATHEMATICA), null);

        this.dataset = datasetPath;
        this.symbolicMaple = symbolicResultsMaple;
        this.numericMaple = numericResultsMaple;
        this.symbolicMath = symbolicResultsMath;
        this.numericMath = numericResultsMath;
        this.numericMathSymbSuc = numericSymbSuccResultsMath;
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
//        this.mapleTranslator.init(GlobalPaths.PATH_REFERENCE_DATA);

        LOG.info("Init Mathematica translator");
//        this.mathematicaTranslator.init(GlobalPaths.PATH_REFERENCE_DATA);

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

        LOG.info("Load numeric test results of Mathematica Symbolically Verified");
        Files.walk(numericMathSymbSuc)
                .filter( f -> Files.isRegularFile(f) )
                .forEach( p -> loadResults(p, NUMERIC_PATTERN, mathNum, true));
    }

    public void setRange(int start, int end) {
        range = new int[]{start, end};
    }

    private void loadResults(Path path, Pattern pattern, HashMap<String, String> lib) {
        loadResults(path, pattern, lib, false);
    }

    private void loadResults(Path path, Pattern pattern, HashMap<String, String> lib, boolean skip) {
        try {
            Files.lines(path)
                    .skip(1)
                    .forEach(l -> {
                        Matcher m = pattern.matcher(l);
                        if ( m.matches() ) {
                            String lineStr = m.group(1);
                            String res = m.group(2);
                            if (!skip || !res.contains("Skip")) {
                                lib.put(lineStr, res);
                            }
                        } else {
                            m = FALL_BACK_PATTERN.matcher(l);
                            if ( m.matches() ) {
                                String lineStr = m.group(1);
                                String res = m.group(2);
                                if ( res.length() > CHAR_LIMIT/2 ) {
                                    res = res.substring(0, CHAR_LIMIT/2);
                                }
                                if (!skip || !res.contains("Skip")) {
                                    lib.put(lineStr, res);
                                }
                            }
                        }
                    });
        } catch ( IOException ioe ) {
            LOG.error("Cannot load results from " + path, ioe);
        }
    }

    @Override
    public LinkedList<Case> loadTestCases() {
        HashMap<Integer, String> labelLib = new HashMap<>();
        HashMap<Integer, String> skippedLinesInfo = new HashMap<>();
        return super.loadTestCases(
                range,
                new HashSet<ID>(),
                dataset,
                labelLib,
                skippedLinesInfo,
                false
        );
    }

    @Override
    public void performSingleTest(Case testCase) { }

    @Override
    public EvaluationConfig getConfig() {
        return null;
    }

    @Override
    public HashMap<Integer, String> getLabelLibrary() {
        return null;
    }

    @Override
    public LinkedList<String>[] getLineResults() {
        return new LinkedList[0];
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

    public void printFind( String element ) throws IOException {
        Pattern elPattern = Pattern.compile(element);
        Pattern url = Pattern.compile("url\\{(.*?)}");
        int[] lineNumber = new int[]{0};

        int[] hits = new int[]{0};
        LinkedList<String> list = new LinkedList<>();

        Files.lines(dataset)
                .peek(l -> lineNumber[0]++)
                .filter( l -> {
                    String[] part = l.split("url");
                    Matcher m = elPattern.matcher(part[0]);
                    return m.find();
                })
                .forEach( l -> {
                    String id = Integer.toString(lineNumber[0]);
                    String mathRes = mathNum.get(id);
//                    String maplRes = mapleNum.get(id);
                    Matcher urlM = url.matcher(l);
                    String urlStr = urlM.find() ? urlM.group(1) : "";

                    if ( mathRes != null && !mathRes.contains("Skip") && !mathRes.contains("Error") && !mathRes.contains("Successful") ) {
                        System.out.println(lineNumber[0] + " " + urlStr + ": " + mathRes.substring(0, Math.min(100, mathRes.length())));
                        hits[0]++;
                        list.add(id);
                    }
                });
        System.out.println("Found: " + hits[0]);
        System.out.println(list);
    }

    public void generateTable( Path outputFile ) throws IOException {
//        int[] line = new int[]{0};
//        LinkedList<LinkedList<Case>> allCases = new LinkedList<>();
        LOG.info("Load dataset.");

        LinkedList<Case> allCases = loadTestCases();
//        Files.lines(dataset)
//                .sequential()
//                .peek(l -> line[0]++)
//                .forEach(l -> {
//                    LinkedList<Case> cases = CaseAnalyzer.analyzeLine(l, line[0], symbolDefinitionLibrary);
//                    if ( cases != null && !cases.isEmpty() ) allCases.add(cases);
//                });

        LOG.info("Start analyzing the data...");
        int fileID = 1;
        Path filePath = outputFile.resolve(fileID+".txt");

        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(filePath.toFile()));
            writer.write("{| class=\"wikitable sortable\"\n|-\n");
            writer.write("! DLMF !! Formula !! Maple !! Mathematica !! Symbolic<br>Maple !! Symbolic<br>Mathematica !! Numeric<br>Maple !! Numeric<br>Mathematica\n|-\n");

            HashMap<Integer, Integer> caseNumberLib = new HashMap<>();

//            for ( LinkedList<Case> lineCase : allCases ) {
//                Case c = lineCase.get(0);
            for ( Case c : allCases ) {
                int number = caseNumberLib.computeIfAbsent( c.getLine(), lineNumber -> 1 );

                int currentSec = Integer.parseInt(c.getEquationLabel().split("\\.")[0]);
                if ( currentSec > fileID ) {
                    writer.write("|}");
                    writer.close();
                    fileID = currentSec;
                    filePath = outputFile.resolve(fileID+".txt");
                    writer = new BufferedWriter(new FileWriter(filePath.toFile()));
                    writer.write("{| class=\"wikitable sortable\"\n|-\n");
                    writer.write("! DLMF !! Formula !! Maple !! Mathematica !! Symbolic<br>Maple !! Symbolic<br>Mathematica !! Numeric<br>Maple !! Numeric<br>Mathematica\n|-\n");
                }

                String id = ""+c.getLine();
                char a = 'a';
                for ( int i = 1; i < number; i++ ) {
                    id = c.getLine() + "-" + a;
                    a++;
                }

                caseNumberLib.put(c.getLine(), number+1);
                singleCase(id, c, writer);
            }

            writer.write("|}");
        } catch ( IOException ioe ) {
            LOG.error("Unable to write file.", ioe);
        } finally {
            if ( writer != null ) writer.close();
        }
    }

    private void singleCase( String id, Case c, BufferedWriter writer ) throws IOException {
        LOG.info("Writing " + id);

        String originalExpression = c.getLHS() + " " + c.getRelation().getTexSymbol() + " " + c.getRHS();
        c = c.replaceSymbolsUsed(symbolDefinitionLibrary);
        String expr = c.getLHS() + " " + c.getRelation().getTexSymbol() + " " + c.getRHS();
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
        String numericMaple = getNumericResultString( true, id );
        String numericMath = getNumericResultString( false, id );

        String line = String.format(
                TABLE_LINE,
                label, label,
                qid, originalExpression,
                maple, mathematica,
                symbMaple, symbMath,
                numericMaple, numericMath
        );

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

        Matcher startM = NumericalDifferencesAnalyzer.failedNumericPattern.matcher(result);

        if ( startM.find() ) {
            if ( result.contains("Error") ) return "Error";

            StringBuilder sb = new StringBuilder();
            int pFailed = Integer.parseInt(startM.group(1));
            int tFailed = Integer.parseInt(startM.group(2));

            startM.appendTail(sb);
            result = sb.toString();

            int counter = 0;
            Matcher m = elPattern.matcher(result);
            StringBuilder list = new StringBuilder();
            while ( m.find() ) {
                if ( counter >= LIMIT_ENTRIES ) {
                    // too many entries
                    list.append("... skip entries to safe data<br>");
                    break;
                }
                list.append("<code>").append(m.group(1)).append(" <- {").append(m.group(2)).append("}</code><br>");
                counter++;
            }

            String failedStr = "Failed [" + pFailed + " / " + tFailed + "]";
            list.setLength( Math.min(CHAR_LIMIT, list.length()) );
            return String.format(COLLAPSE_ELEMENT, failedStr, list.toString());
        } else {
            if ( result.contains("Error") ) return "Error";
            else return result;
        }
    }

    public static void main(String[] args) throws IOException, InitTranslatorException {
        TranslationWikidataTableGenerator t = new TranslationWikidataTableGenerator(
                Paths.get("/home/andreg-p/data/Howard/together.txt"),
                Paths.get("misc/Results/MapleSymbolic"),
                Paths.get("misc/Results/MapleNumeric"),
                Paths.get("misc/Results/MathematicaSymbolic"),
                Paths.get("misc/Results/MathematicaNumeric"),
                Paths.get("misc/Results/MathematicaNumericSymbolicSuccessful"),
                Paths.get("/home/andreg-p/data/Howard/formulaQ.csv")
        );

        t.setRange(0, 9978);
        t.init();
//        t.printFind("\\\\ell[^a-zA-Z]");
        t.generateTable(Paths.get("misc/Mediawiki"));
    }
}
