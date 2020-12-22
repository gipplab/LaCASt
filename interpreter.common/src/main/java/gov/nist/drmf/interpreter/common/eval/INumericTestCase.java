package gov.nist.drmf.interpreter.common.eval;

import gov.nist.drmf.interpreter.common.interfaces.IConstraintTranslator;

import java.util.List;

/**
 * @author Andre Greiner-Petter
 */
public interface INumericTestCase {
    /**
     * Returns the equation label of the test case
     * or null, if there is no label.
     * @return the label of the test equation
     */
    default String getEquationLabel() {
        return null;
    }

    /**
     * Get the translated constraints of this test case
     * @param translator the translator to use for the constraints
     * @return list of translated constraints
     */
    default List<String> getConstraints(IConstraintTranslator translator) {
        return getConstraints(translator, getEquationLabel());
    }

    /**
     * Get the translated constraints of this test case
     * @param translator the translator to use for the constraints
     * @param label the DLMF label if any (or null if there is none)
     * @return list of translated constraints
     */
    List<String> getConstraints(IConstraintTranslator translator, String label);

    /**
     * Get the constraint variables of this test case
     * @param translator the translator to use for the constraints
     * @return the variables in the syntax of the CAS
     */
    default List<String> getConstraintVariables(IConstraintTranslator translator) {
        return getConstraintVariables(translator, getEquationLabel());
    }

    /**
     * Get the constraint variables of this test case
     * @param translator the translator to use for the constraints
     * @param label the DLMF label if any (or null)
     * @return the variables in the syntax of the CAS
     */
    List<String> getConstraintVariables(IConstraintTranslator translator, String label);

    /**
     * The constraint values.
     * @return the values of the constraints
     */
    List<String> getConstraintValues();
}
