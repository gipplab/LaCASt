package gov.nist.drmf.interpreter.common.eval;

import gov.nist.drmf.interpreter.common.interfaces.IConstraintTranslator;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Andre Greiner-Petter
 */
public class NumericalTest implements Serializable {

    private final String testExpression;

    private List<String> testValues,
            constraints,
            constraintVariables,
            constraintVariablesValues,
            extraVariables,
            extraVariablesValues;

    private int precision = 10;
    private int maxCombis = 100;

    private String postProcessingMethodName;

    private boolean skipClassicAbortion = false;

    private Set<String> variables;

    private Set<String> requiredPackages;

    private final String lhs, rhs;

    public NumericalTest(String lhs, String rhs, String testExpression) {
        this.testExpression = testExpression;
        this.requiredPackages = new HashSet<>();
        this.lhs = lhs;
        this.rhs = rhs;
    }

    public NumericalTest(
            String lhs, String rhs,
            String testExpression,
            INumericTestCase c,
            NumericalConfig config,
            IConstraintTranslator translator
    ) {
        this(lhs, rhs, testExpression);

        String label = c.getEquationLabel();
        testValues = config.getListOfNumericalValues(translator, label);
        constraints = c.getConstraints(translator, label);
        constraintVariables = c.getConstraintVariables(translator, label);
        constraintVariablesValues = c.getConstraintValues();
        extraVariables = config.getListOfSpecialVariables(translator);
        extraVariablesValues = config.getListOfSpecialVariableValues(translator);

        precision = config.getPrecision();
        maxCombis = config.getMaximumNumberOfCombs();
    }

    NumericalTest setTestValues(List<String> testValues) {
        this.testValues = testValues;
        return this;
    }

    NumericalTest setConstraints(List<String> constraints) {
        this.constraints = constraints;
        return this;
    }

    NumericalTest setConstraintVariables(List<String> constraintVariables) {
        this.constraintVariables = constraintVariables;
        return this;
    }

    NumericalTest setConstraintVariablesValues(List<String> constraintVariablesValues) {
        this.constraintVariablesValues = constraintVariablesValues;
        return this;
    }

    NumericalTest setExtraVariables(List<String> extraVariables) {
        this.extraVariables = extraVariables;
        return this;
    }

    NumericalTest setExtraVariablesValues(List<String> extraVariablesValues) {
        this.extraVariablesValues = extraVariablesValues;
        return this;
    }

    NumericalTest setPrecision(int precision) {
        this.precision = precision;
        return this;
    }

    NumericalTest setMaxCombis(int maxCombis) {
        this.maxCombis = maxCombis;
        return this;
    }

    NumericalTest setSkipClassicAbortion(boolean skipClassicAbortion) {
        this.skipClassicAbortion = skipClassicAbortion;
        return this;
    }

    NumericalTest setRequiredPackages(Set<String> requiredPackages) {
        if ( requiredPackages == null ) this.requiredPackages = new HashSet<>();
        else this.requiredPackages = new HashSet<>(requiredPackages);
        return this;
    }

    public String getLhs() {
        return lhs;
    }

    public String getRhs() {
        return rhs;
    }

    public Set<String> getRequiredPackages() {
        return requiredPackages;
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
