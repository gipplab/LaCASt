package gov.nist.drmf.interpreter.evaluation.core.numeric;

import gov.nist.drmf.interpreter.common.cas.ICASEngineNumericalEvaluator;
import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import gov.nist.drmf.interpreter.common.cas.IComputerAlgebraSystemEngine;
import gov.nist.drmf.interpreter.cas.constraints.IConstraintTranslator;
import gov.nist.drmf.interpreter.evaluation.core.AbstractEvaluator;
import gov.nist.drmf.interpreter.evaluation.common.Status;
import gov.nist.drmf.interpreter.evaluation.core.symbolic.AbstractSymbolicEvaluator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.intellij.lang.annotations.Language;

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

    public void setGlobalAssumptions(List<String> globalAssumptions) {
        numericalEvaluator.setGlobalAssumptions(globalAssumptions);
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
            NumericalTest test
    ) throws ComputerAlgebraSystemEngineException, IllegalArgumentException {
        LOG.info("Prepare numerical test.");

        // store variables first
        numericalEvaluator.storeVariables(
                test.getVariables(),
                test.getTestValues()
        );

        // next, store constraint variables extracted from blueprints
        numericalEvaluator.storeConstraintVariables(
                test.getConstraintVariables(),
                test.getConstraintVariablesValues()
        );

        // next, store special variables (such as k should be integer)
        numericalEvaluator.storeExtraVariables(
                test.getExtraVariables(),
                test.getExtraVariablesValues()
        );

        // next, store the actual constraints
        String constraintN = numericalEvaluator.setConstraints(test.getConstraints());


        // finally, generate all test cases that fit the constraints
        String testValuesN = numericalEvaluator.buildTestCases(
                constraintN,
                test.getMaxCombis()
        );

//        Thread abortThread = null;
//        if ( !test.skipClassicAbortion() ) {
//            abortThread = getAbortionThread(numericalEvaluator, getTimeoutSeconds()*2);
//            abortThread.start();
//        }

        // perform the test
        return numericalEvaluator.performNumericalTests(
                test.getTestExpression(),
                testValuesN,
                test.getPostProcessingMethodName(),
                test.getPrecision()
        );

//        if ( abortThread != null ) abortThread.interrupt();
//        return res;
    }

    public boolean isAbortedResult(T result) {
        return numericalEvaluator.wasAborted(result);
    }

//    @Override
//    public String getOverviewString() {
//        return Status.buildNumericalString();
//    }

    public ICASEngineNumericalEvaluator.ResultType testResult(T results) throws ComputerAlgebraSystemEngineException {
        return numericalEvaluator.getStatusOfResult(results);
    }

    public ICASEngineNumericalEvaluator<?> getNumericalEvaluator() {
        return numericalEvaluator;
    }

    public Set<ID> getSpecificResults(Path dataset, @Language("RegExp") String resultString) {
        Set<ID> set = new HashSet<>();
        if ( dataset == null || !Files.exists(dataset) ) return set;
        try {
            Files.lines(dataset)
                .map( l -> {
                    Matcher m = AbstractSymbolicEvaluator.SYMBOLIC_LINE_PATTERN.matcher(l);
                    if ( m.matches() ) {
                        if ( m.group(2).matches(resultString) ) return m.group(1);
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
