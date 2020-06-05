package gov.nist.drmf.interpreter.evaluation.core.symbolic;

import gov.nist.drmf.interpreter.common.cas.ICASEngineSymbolicEvaluator;
import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import gov.nist.drmf.interpreter.common.cas.IComputerAlgebraSystemEngine;
import gov.nist.drmf.interpreter.cas.constraints.IConstraintTranslator;
import gov.nist.drmf.interpreter.evaluation.core.AbstractEvaluator;
import gov.nist.drmf.interpreter.evaluation.core.EvaluationConfig;
import gov.nist.drmf.interpreter.evaluation.common.Status;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.regex.Pattern;

/**
 * @author Andre Greiner-Petter
 */
public abstract class AbstractSymbolicEvaluator<T> extends AbstractEvaluator<T> {
    private static final Logger LOG = LogManager.getLogger(AbstractSymbolicEvaluator.class.getName());

    public static final Pattern SYMBOLIC_LINE_PATTERN = Pattern.compile(
            "^(\\d+-?[a-z]?)(?: \\[.*])?: ([A-Za-z]*) .*$"
    );

    private ICASEngineSymbolicEvaluator<T> symbolicEvaluator;
    private ISymbolicTestCases[] symbolicTestCases;

    public AbstractSymbolicEvaluator(
            IConstraintTranslator forwardTranslator,
            IComputerAlgebraSystemEngine<T> engine,
            ICASEngineSymbolicEvaluator<T> symbolicEvaluator,
            ISymbolicTestCases[] testCases
    ) {
        super( forwardTranslator, engine );
        this.symbolicEvaluator = symbolicEvaluator;
        this.symbolicTestCases = testCases;
    }

    public T simplify( String command ) throws ComputerAlgebraSystemEngineException {
        Thread abortThread = getAbortionThread(symbolicEvaluator);
        abortThread.start();
        T result = symbolicEvaluator.simplify(command, getRequiredPackages());
        // waits for an answer, once the answer is received, we finished the process
        abortThread.interrupt();

        return result;
    }

    public T simplify( String command, String assumption ) throws ComputerAlgebraSystemEngineException {
        Thread abortThread = getAbortionThread(symbolicEvaluator);
        abortThread.start();
        LOG.info("Started abortion thread.");

        T result = null;
        if ( assumption == null || assumption.isEmpty() )
            result = symbolicEvaluator.simplify(command, getRequiredPackages());
        else
            result = symbolicEvaluator.simplify(command, assumption, getRequiredPackages());

        // waits for an answer, once the answer is received, we finished the process
        abortThread.interrupt();

        return result;
    }

    public boolean isAbortedResult(T result) {
        return symbolicEvaluator.wasAborted(result);
    }

    public ISymbolicTestCases[] getSymbolicTestCases() {
        return this.symbolicTestCases;
    }

    public boolean validOutCome(T in, String expect) {
        return symbolicEvaluator.isAsExpected(in, expect);
    }

    @Override
    protected String getResults(
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

        sb.append(Arrays.toString(getSymbolicTestCases()));
        sb.append(NL);

        return buildResults(
                sb,
                labelLib,
                config.showDLMFLinks(),
                config.getSubSetInterval(),
                lineResults
        );
    }
}
