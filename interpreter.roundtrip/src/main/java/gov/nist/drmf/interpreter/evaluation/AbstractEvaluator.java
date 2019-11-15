package gov.nist.drmf.interpreter.evaluation;

import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.grammar.IComputerAlgebraSystemEngine;
import gov.nist.drmf.interpreter.common.grammar.ITranslator;
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

    private boolean interrupt = false;

    private ITranslator forwardTranslator;
    private IComputerAlgebraSystemEngine<T> engine;

    public AbstractEvaluator(ITranslator forwardTranslator, IComputerAlgebraSystemEngine<T> engine) {
        this.forwardTranslator = forwardTranslator;
        this.engine = engine;
    }

    public String forwardTranslate(String in) throws TranslationException {
        return forwardTranslator.translate(in);
    }

    public T enterEngineCommand(String cmd) throws ComputerAlgebraSystemEngineException {
        return engine.enterCommand(cmd);
    }

    public abstract void init() throws Exception;

    public abstract void performSingleTest(Case testCase);

    public void performAllTests(LinkedList<Case> testCases) {
        for ( Case test : testCases ) {
            performSingleTest(test);
        }
    }

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
}
