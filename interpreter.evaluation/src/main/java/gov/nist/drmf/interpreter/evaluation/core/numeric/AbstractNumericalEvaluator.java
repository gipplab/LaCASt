package gov.nist.drmf.interpreter.evaluation.core.numeric;

import gov.nist.drmf.interpreter.common.cas.ICASEngineNumericalEvaluator;
import gov.nist.drmf.interpreter.common.cas.IComputerAlgebraSystemEngine;
import gov.nist.drmf.interpreter.common.eval.INumericTestCalculator;
import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import gov.nist.drmf.interpreter.common.interfaces.IConstraintTranslator;
import gov.nist.drmf.interpreter.evaluation.core.AbstractEvaluator;
import gov.nist.drmf.interpreter.evaluation.core.symbolic.AbstractSymbolicEvaluator;
import gov.nist.drmf.interpreter.pom.extensions.PrintablePomTaggedExpression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.intellij.lang.annotations.Language;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;

/**
 * @author Andre Greiner-Petter
 */
public abstract class AbstractNumericalEvaluator extends AbstractEvaluator implements INumericTestCalculator {
    private static final Logger LOG = LogManager.getLogger(AbstractNumericalEvaluator.class.getName());

    private final ICASEngineNumericalEvaluator numericalEvaluator;

    private String[] scripts;

    public AbstractNumericalEvaluator(
            IConstraintTranslator<PrintablePomTaggedExpression> forwardTranslator,
            IComputerAlgebraSystemEngine engine,
            ICASEngineNumericalEvaluator numericalEvaluator
    ) {
        super(forwardTranslator, engine);
        this.numericalEvaluator = numericalEvaluator;
    }

    @Override
    public ICASEngineNumericalEvaluator getNumericEvaluator(){
        return this.numericalEvaluator;
    }

    public void setGlobalNumericAssumptions(List<String> assumptions) throws ComputerAlgebraSystemEngineException {
        numericalEvaluator.setGlobalNumericAssumptions(assumptions);
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
