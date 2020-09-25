package gov.nist.drmf.interpreter.evaluation.core.numeric;

import gov.nist.drmf.interpreter.cas.constraints.IConstraintTranslator;
import gov.nist.drmf.interpreter.evaluation.common.Case;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author Andre Greiner-Petter
 */
public class NumericalTest {

    private final String testExpression;

    private final List<String> testValues,
            constraints,
            constraintVariables,
            constraintVariablesValues,
            extraVariables,
            extraVariablesValues;

    private final int precision;
    private final int maxCombis;

    private String postProcessingMethodName;

    private boolean skipClassicAbortion = false;

    private Set<String> variables;

    public NumericalTest(
            String testExpression,
            Case c,
            NumericalConfig config,
            IConstraintTranslator translator
    ) {
        this.testExpression = testExpression;

        String label = c.getEquationLabel();
        testValues = config.getListOfNumericalValues(translator, label);
        constraints = c.getConstraints(translator, label);
        constraintVariables = c.getConstraintVariables(translator, label);
        constraintVariablesValues = c.getConstraintValues();
        extraVariables = config.getListOfSpecialVariables(translator, label);
        extraVariablesValues = config.getListOfSpecialVariableValues(translator, label);

        precision = config.getPrecision();
        maxCombis = config.getMaximumNumberOfCombs();
    }

    public Set<String> getVariables() {
        return variables;
    }

    public void setVariables(Set<String> variables) {
        this.variables = variables;
    }

    public void setPostProcessingMethodName(String postProcessingMethodName){
        this.postProcessingMethodName = postProcessingMethodName;
    }

    public void setSkipClassicAbortion() {
        this.skipClassicAbortion = true;
    }

    public String getTestExpression() {
        return testExpression;
    }

    public List<String> getTestValues() {
        return testValues;
    }

    public List<String> getConstraints() {
        return constraints;
    }

    public List<String> getConstraintVariables() {
        return constraintVariables;
    }

    public List<String> getConstraintVariablesValues() {
        return constraintVariablesValues;
    }

    public List<String> getExtraVariables() {
        return extraVariables;
    }

    public List<String> getExtraVariablesValues() {
        return extraVariablesValues;
    }

    public String getPostProcessingMethodName() {
        return postProcessingMethodName;
    }

    public int getPrecision() {
        return precision;
    }

    public int getMaxCombis() {
        return maxCombis;
    }

    public boolean skipClassicAbortion() {
        return skipClassicAbortion;
    }
}
