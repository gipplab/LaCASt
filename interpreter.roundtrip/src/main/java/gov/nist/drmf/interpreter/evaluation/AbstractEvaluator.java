package gov.nist.drmf.interpreter.evaluation;

import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.grammar.IComputerAlgebraSystemEngine;
import gov.nist.drmf.interpreter.common.grammar.ITranslator;
import gov.nist.drmf.interpreter.constraints.Constraints;
import gov.nist.drmf.interpreter.constraints.IConstraintTranslator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

/**
 * @author Andre Greiner-Petter
 */
public abstract class AbstractEvaluator<T> {
    private static final Logger LOG = LogManager.getLogger(AbstractEvaluator.class.getName());

    public final static String NL = System.lineSeparator();

    private IConstraintTranslator forwardTranslator;
    private IComputerAlgebraSystemEngine<T> engine;

    public AbstractEvaluator(
            IConstraintTranslator forwardTranslator,
            IComputerAlgebraSystemEngine<T> engine
    ) {
        this.forwardTranslator = forwardTranslator;
        this.engine = engine;
    }

    public String forwardTranslate(String in) throws TranslationException {
        return forwardTranslator.translate(in);
    }

    public T enterEngineCommand(String cmd) throws ComputerAlgebraSystemEngineException {
        return engine.enterCommand(cmd);
    }

    public IConstraintTranslator getThisConstraintTranslator() {
        return forwardTranslator;
    }

    public void forceGC() throws ComputerAlgebraSystemEngineException {
        this.engine.forceGC();
    }

    public abstract void performSingleTest(Case testCase);

    public void performAllTests(LinkedList<Case> testCases) {
        for ( Case test : testCases ) {
            performSingleTest(test);
        }
    }

    public abstract LinkedList<Case> loadTestCases();

    public LinkedList<Case> loadTestCases(
            int[] subset,
            Set<Integer> skipLines,
            Path dataset,
            HashMap<Integer, String> labelLib,
            HashMap<Integer, String> skippedLinesInfo
    ) {
        LinkedList<Case> testCases = new LinkedList<>();
        int[] currLine = new int[] {0};

        try (BufferedReader br = Files.newBufferedReader(dataset)) {
            Stream<String> lines = br.lines();

            int start = subset[0];
            int limit = subset[1];

            lines.sequential()
                    .peek(l -> currLine[0]++) // line counter
                    .filter(l -> start <= currLine[0] && currLine[0] < limit) // filter by limits
                    .filter(l -> !skipLines.contains(currLine[0]) ) // skip entries if wanted
                    .flatMap(l -> {
                        LinkedList<Case> cc = CaseAnalyzer.analyzeLine(l, currLine[0]);
                        if (cc != null && !cc.isEmpty()) {
                            Case c = cc.get(0);
                            labelLib.put(c.getLine(), c.getDlmf());
                            return cc.stream();
                        } else {
                            System.out.println(currLine[0] + ": unable to extract test case.");
                            skippedLinesInfo.put(currLine[0], "Skipped - Because of NULL element after parsing line.");
                            Status.SKIPPED.add();
                            return null;
                        }
                    })
                    .filter( Objects::nonNull )
                    .forEach(testCases::add);
            return testCases;
        } catch (IOException ioe) {
            LOG.fatal("Cannot load dataset!", ioe);
            return null;
        }
    }

    public abstract EvaluationConfig getConfig();
    public abstract HashMap<Integer, String> getLabelLibrary();
    public abstract LinkedList<String>[] getLineResults();

    public void writeResults() throws IOException {
        String results = getResults(
                this.getConfig(),
                this.getLabelLibrary(),
                this.getLineResults()
        );
        Files.write(
                this.getConfig().getOutputPath(),
                results.getBytes()
        );
    }

    private String getResults(
            EvaluationConfig config,
            HashMap<Integer, String> labelLib,
            LinkedList<String>[] lineResults
    ){
        StringBuffer sb = new StringBuffer();

        sb.append("Overall: ");
        sb.append(Status.buildString());
        sb.append(" for test expression: ");
        sb.append(config.getTestExpression());
        sb.append(NL);

        sb.append(Arrays.toString(SymbolicMapleEvaluatorTypes.values()));
        sb.append(NL);

        return buildResults(
                sb,
                labelLib,
                config.showDLMFLinks(),
                config.getSubSetInterval(),
                lineResults
        );
    }

    private String buildResults(
            StringBuffer sb,
            HashMap<Integer, String> labelLib,
            boolean showDLMF,
            int[] limits,
            LinkedList<String>[] lineResults){
        int start = limits[0];
        int limit = limits[1];

        for ( int i = start; i < lineResults.length && i < limit; i++ ){
            sb.append(i);

            LinkedList<String> lineResult = lineResults[i];
            boolean first = true;
            Character c = 'a';

            if ( lineResults[i] == null ){
                sb.append(": Skipped (is null)").append(NL);
                return sb.toString();
            }

            for ( String s : lineResult ) {
                if ( !first ) {
                    sb.append(i+"-"+c);
                    c++;
                } else first = false;

                String dlmf = labelLib.get(i);
                if ( dlmf != null && showDLMF ){
                    sb.append(" [").append(dlmf).append("]: ");
                } else sb.append(": ");

                sb.append(s);
                sb.append(NL);
            }


        }
        return sb.toString();
    }
}
