package gov.nist.drmf.interpreter.evaluation.core;

import gov.nist.drmf.interpreter.common.eval.EvaluationConfig;
import gov.nist.drmf.interpreter.common.interfaces.IConstraintTranslator;
import gov.nist.drmf.interpreter.common.cas.IComputerAlgebraSystemEngine;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.exceptions.InitTranslatorException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.core.DLMFTranslator;
import gov.nist.drmf.interpreter.evaluation.common.Case;
import gov.nist.drmf.interpreter.evaluation.common.CaseAnalyzer;
import gov.nist.drmf.interpreter.evaluation.common.SimpleCase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Andre Greiner-Petter
 */
public class SampleTranslator extends AbstractEvaluator {
    private static final Logger LOG = LogManager.getLogger(SampleTranslator.class.getName());
    private List<SimpleCase> cases;

    private List<Integer> testCases;

    public SampleTranslator(IConstraintTranslator forwardTranslator, IComputerAlgebraSystemEngine engine) throws IOException {
        super(forwardTranslator, engine);
        List<String> lines = Files.readAllLines(
                Paths.get("/home/andreg-p/data/DLMF-AI/random100SampleLineIDs.txt")
        );
        testCases = lines.stream().map( Integer::parseInt ).collect(Collectors.toList());
        cases = loadSimpleTestCases();
    }

    public LinkedList<LineTranslation> translate(List<Integer> tests) {
        LinkedList<LineTranslation> translations = new LinkedList<>();

        List<SimpleCase> filteredTestCases = cases.stream()
                .filter( c -> tests.contains(c.getLine()) ).collect(Collectors.toList());
        for ( SimpleCase c : filteredTestCases ) {
            try {
                LOG.info("Start translating line " + c.getLine() + ": " + c.getExpression());
                String trans = forwardTranslate(c.getExpression(), c.getEquationLabel()).getTranslatedExpression();
                LOG.info("Successfully translated line " + c.getLine() + ": " + c.getExpression() + "\nTo: " + trans);
                translations.add(new LineTranslation(
                        c.getLine(),
                        c.getExpression(),
                        trans,
                        c.getLabel() != null ? c.getLabel().getHyperlink() : c.getEquationLabel()
                ));
            } catch (TranslationException te) {
                LOG.error("Unable to translate line " + c.getLine() + ": " + te.toString());
                translations.add(new LineTranslation(c.getLine(), c.getExpression(), "Error - " + te.toString(), c.getLabel() != null ? c.getLabel().getHyperlink() : c.getEquationLabel()));
            } catch (Exception e) {
                LOG.error("Unable to translate line " + c.getLine(), e);
                translations.add(new LineTranslation(c.getLine(), c.getExpression(), "Error - " + e.getMessage(), c.getLabel() != null ? c.getLabel().getHyperlink() : c.getEquationLabel()));
            }
        }

        return translations;
    }

    @Override
    public void performSingleTest(Case testCase) {
        // nothing to do here
    }

    @Override
    public LinkedList<Case> loadTestCases() {
        return null;
    }

    public List<SimpleCase> loadSimpleTestCases() {
        // nothing to do here
        try ( BufferedReader br = Files.newBufferedReader(
                Paths.get("/home/andreg-p/data/Howard/together.txt"))
        ) {
            int[] currLine = new int[] {0};
            List<SimpleCase> cases = br.lines()
                    .peek( l -> currLine[0]++ )
                    .map( l -> CaseAnalyzer.extractRawLines(l, currLine[0]))
                    .collect(Collectors.toList());
            return cases;
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return null;
        }
    }

    @Override
    public EvaluationConfig getConfig() {
        // nothing to do here
        return null;
    }

    @Override
    public HashMap<Integer, String> getLabelLibrary() {
        // nope
        return null;
    }

    @Override
    public LinkedList<String>[] getLineResults() {
        // again nothing
        return new LinkedList[0];
    }

    private static class LineTranslation {
        private int line;
        private String semanticLaTeX;
        private String translation;
        private String dlmfLink;

        public LineTranslation(int line, String semanticLaTeX, String translation, String dlmfLink) {
            this.line = line;
            this.semanticLaTeX = semanticLaTeX;
            this.translation = translation;
            this.dlmfLink = dlmfLink;
        }
    }

    public static void main(String[] args) throws InitTranslatorException, IOException {
        DLMFTranslator dlmfTranslator = new DLMFTranslator(Keys.KEY_MATHEMATICA);
        SampleTranslator st = new SampleTranslator(dlmfTranslator, null);
        LinkedList<LineTranslation> translations = st.translate(st.testCases);

        Files.deleteIfExists(Paths.get("/home/andreg-p/data/DLMF-AI/lacast-translations.txt"));

        try (BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(
                        new FileOutputStream(
                                Paths.get("/home/andreg-p/data/DLMF-AI/lacast-translations.txt").toFile())))
        ) {
            for ( LineTranslation translation : translations ) {
                bw.write(translation.translation + "\n");
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        Files.deleteIfExists(Paths.get("/home/andreg-p/data/DLMF-AI/dlmf-labels.txt"));
        try (BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(
                        new FileOutputStream(
                                Paths.get("/home/andreg-p/data/DLMF-AI/dlmf-labels.txt").toFile())))
        ) {
            for ( LineTranslation translation : translations ) {
                bw.write(translation.dlmfLink + "\n");
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        Files.deleteIfExists(Paths.get("/home/andreg-p/data/DLMF-AI/semanticTeX.txt"));
        try (BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(
                        new FileOutputStream(
                                Paths.get("/home/andreg-p/data/DLMF-AI/semanticTeX.txt").toFile())))
        ) {
            for ( LineTranslation translation : translations ) {
                bw.write(translation.semanticLaTeX + "\n");
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}
