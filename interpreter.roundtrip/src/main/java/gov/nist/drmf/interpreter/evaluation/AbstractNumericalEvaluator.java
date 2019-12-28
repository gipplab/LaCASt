package gov.nist.drmf.interpreter.evaluation;

import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import gov.nist.drmf.interpreter.common.grammar.IComputerAlgebraSystemEngine;
import gov.nist.drmf.interpreter.constraints.IConstraintTranslator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;

/**
 * @author Andre Greiner-Petter
 */
public abstract class AbstractNumericalEvaluator<T> extends AbstractEvaluator<T> {
    private static final Logger LOG = LogManager.getLogger(AbstractNumericalEvaluator.class.getName());

    private ICASEngineNumericalEvaluator<T> numericalEvaluator;

    private String[] scripts;

    public AbstractNumericalEvaluator(
            IConstraintTranslator forwardTranslator,
            IComputerAlgebraSystemEngine<T> engine,
            ICASEngineNumericalEvaluator<T> numericalEvaluator
    ) {
        super(forwardTranslator, engine);
        this.numericalEvaluator = numericalEvaluator;
    }

    public void setUpScripts(String... scripts) throws ComputerAlgebraSystemEngineException {
        this.scripts = scripts;
        reloadScripts();
    }

    public void reloadScripts() throws ComputerAlgebraSystemEngineException {
        for ( String script : scripts ) {
            enterEngineCommand(script);
        }
    }

    public T performNumericalTest(
            String testExpression,
            List<String> testValues,
            List<String> constraints,
            List<String> constraintVariables,
            List<String> constraintVariablesValues,
            List<String> extraVariables,
            List<String> extraVariablesValues,
            String postProcessingMethodName,
            int precision,
            int maxCombis
    ) throws ComputerAlgebraSystemEngineException, IllegalArgumentException {
        LOG.info("Prepare numerical test.");

        // store variables first
        String varsN = numericalEvaluator.storeVariables(testExpression, testValues);

        // next, store constraint variables extracted from blueprints
        String constraintVarsN = numericalEvaluator.storeConstraintVariables(
                varsN,
                constraintVariables,
                constraintVariablesValues
        );

        // next, store special variables (such as k should be integer)
        String extraVariablesN = numericalEvaluator.storeExtraVariables(
                varsN,
                extraVariables,
                extraVariablesValues
        );

        // next, store the actual constraints
        String constraintN = numericalEvaluator.setConstraints(constraints);

        Thread abortThread = getAbortionThread(numericalEvaluator, DEFAULT_TIMEOUT_MS*2);
        abortThread.start();

        // finally, generate all test cases that fit the constraints
        String testValuesN = numericalEvaluator.buildTestCases(
                constraintN,
                varsN,
                constraintVarsN,
                extraVariablesN,
                maxCombis
        );

        // perform the test
        T res = numericalEvaluator.performNumericalTests(
                testExpression,
                testValuesN,
                postProcessingMethodName,
                precision
        );

        abortThread.interrupt();
        return res;
    }

    public boolean isAbortedResult(T result) {
        return numericalEvaluator.wasAborted(result);
    }

    @Override
    public String getOverviewString() {
        return Status.buildNumericalString();
    }

    public ICASEngineNumericalEvaluator.ResultType testResult(T results) throws ComputerAlgebraSystemEngineException {
        return numericalEvaluator.getStatusOfResult(results);
    }

    public ICASEngineNumericalEvaluator getNumericalEvaluator() {
        return numericalEvaluator;
    }

    public Set<ID> getFailures(Path dataset) {
        Set<ID> set = new HashSet<>();
        if ( dataset == null || !Files.exists(dataset) ) return set;
        try {
            Files.lines(dataset)
                .map( l -> {
                    Matcher m = AbstractSymbolicEvaluator.SYMBOLIC_LINE_PATTERN.matcher(l);
                    if ( m.matches() ) {
                        if ( m.group(2).equals("Failure") ) return m.group(1);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .map(ID::new)
                .forEach(set::add);
        } catch (IOException e) {
            LOG.error("Cannot load specified symbolic results!");
            e.printStackTrace();
        }
        return set;
    }
}
