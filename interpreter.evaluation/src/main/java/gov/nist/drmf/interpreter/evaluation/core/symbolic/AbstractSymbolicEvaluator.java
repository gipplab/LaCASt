package gov.nist.drmf.interpreter.evaluation.core.symbolic;

import gov.nist.drmf.interpreter.common.cas.ICASEngineSymbolicEvaluator;
import gov.nist.drmf.interpreter.common.eval.ISymbolicTestCases;
import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import gov.nist.drmf.interpreter.common.cas.IComputerAlgebraSystemEngine;
import gov.nist.drmf.interpreter.common.interfaces.IConstraintTranslator;
import gov.nist.drmf.interpreter.evaluation.core.AbstractEvaluator;
import gov.nist.drmf.interpreter.common.eval.EvaluationConfig;
import gov.nist.drmf.interpreter.evaluation.common.Status;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author Andre Greiner-Petter
 */
public abstract class AbstractSymbolicEvaluator extends AbstractEvaluator {
    private static final Logger LOG = LogManager.getLogger(AbstractSymbolicEvaluator.class.getName());

    public static final Pattern SYMBOLIC_LINE_PATTERN = Pattern.compile(
            "^(\\d+-?[a-z]?)(?: \\[.*])?: ([A-Za-z\\s]*).*$"
    );

    private final ICASEngineSymbolicEvaluator symbolicEvaluator;
    private final ISymbolicTestCases[] symbolicTestCases;

    public AbstractSymbolicEvaluator(
            IConstraintTranslator forwardTranslator,
            IComputerAlgebraSystemEngine engine,
            ICASEngineSymbolicEvaluator symbolicEvaluator,
            ISymbolicTestCases[] testCases
    ) {
        super( forwardTranslator, engine );
        this.symbolicEvaluator = symbolicEvaluator;
        this.symbolicTestCases = testCases;
    }

    public ICASEngineSymbolicEvaluator getSymbolicEvaluator() {
        return symbolicEvaluator;
    }

    public ISymbolicTestCases[] getSymbolicTestCases() {
        return this.symbolicTestCases;
    }

    public void setGlobalSymbolicAssumptions(List<String> assumptions) throws ComputerAlgebraSystemEngineException {
        symbolicEvaluator.setGlobalSymbolicAssumptions(assumptions);
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
