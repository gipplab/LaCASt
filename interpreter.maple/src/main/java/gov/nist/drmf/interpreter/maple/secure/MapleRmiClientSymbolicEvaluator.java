package gov.nist.drmf.interpreter.maple.secure;

import gov.nist.drmf.interpreter.common.cas.ICASEngineSymbolicEvaluator;
import gov.nist.drmf.interpreter.common.eval.EvaluatorType;
import gov.nist.drmf.interpreter.common.eval.SymbolicResult;
import gov.nist.drmf.interpreter.common.eval.SymbolicalTest;
import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;

import java.util.List;

/**
 * @author Andre Greiner-Petter
 */
public class MapleRmiClientSymbolicEvaluator implements ICASEngineSymbolicEvaluator {

    private final MapleRmiClient mapleClient;

    MapleRmiClientSymbolicEvaluator(MapleRmiClient mapleClient) {
        this.mapleClient = mapleClient;
    }

    @Override
    public SymbolicResult performSymbolicTest(SymbolicalTest test) {
        return mapleClient.performSymbolicTest(test);
    }

    @Override
    public void setTimeout(EvaluatorType type, double timeoutInSeconds) {
        mapleClient.setTimeout(type, timeoutInSeconds);
    }

    @Override
    public void setGlobalSymbolicAssumptions(List<String> assumptions) throws ComputerAlgebraSystemEngineException {
        mapleClient.setGlobalSymbolicAssumptions(assumptions);
    }
}
