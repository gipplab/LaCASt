package gov.nist.drmf.interpreter.maple.secure;

import gov.nist.drmf.interpreter.common.cas.ICASEngineNumericalEvaluator;
import gov.nist.drmf.interpreter.common.eval.EvaluatorType;
import gov.nist.drmf.interpreter.common.eval.NumericResult;
import gov.nist.drmf.interpreter.common.eval.NumericalTest;
import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;

import java.util.List;

/**
 * @author Andre Greiner-Petter
 */
public class MapleRmiClientNumericEvaluator implements ICASEngineNumericalEvaluator {
    private final MapleRmiClient mapleClient;

    MapleRmiClientNumericEvaluator(MapleRmiClient mapleClient) {
        this.mapleClient = mapleClient;
    }

    @Override
    public NumericResult performNumericTest(NumericalTest test) throws ComputerAlgebraSystemEngineException {
        return mapleClient.performNumericTest(test);
    }

    @Override
    public String generateNumericTestExpression(String expression) {
        return mapleClient.generateNumericTestExpression(expression);
    }

    @Override
    public void setGlobalNumericAssumptions(List<String> assumptions) throws ComputerAlgebraSystemEngineException {
        mapleClient.setGlobalNumericAssumptions(assumptions);
    }

    @Override
    public void setTimeout(EvaluatorType type, double timeoutInSeconds) {
        mapleClient.setTimeout(type, timeoutInSeconds);
    }
}
