package gov.nist.drmf.interpreter.common.interfaces;

import gov.nist.drmf.interpreter.common.exceptions.MinimumRequirementNotFulfilledException;
import gov.nist.drmf.interpreter.common.pojo.SemanticEnhancedAnnotationStatus;

/**
 * @author Andre Greiner-Petter
 */
public interface SemanticallyRanked {
    /**
     * Returns the current rank of this object.
     * @return the semantic rank of this object
     */
    SemanticEnhancedAnnotationStatus getRank();

    /**
     * Checks if this semantically ranked object passed (or equals) the given requirement.
     * If this object passes the minimum requirement nothing happens. If it does not pass the requirement,
     * an {@link MinimumRequirementNotFulfilledException} exception will be thrown
     * @param min the minimum requirement this object should pass. Does nothing if {@link #getRank()} passes the given
     *            requirement
     * @throws MinimumRequirementNotFulfilledException is thrown if this rank does not pass the minimal requirement
     */
    default void requires(SemanticEnhancedAnnotationStatus min) throws MinimumRequirementNotFulfilledException {
        if ( !getRank().hasPassed(min) ) throw new MinimumRequirementNotFulfilledException(min, getRank());
    }
}
