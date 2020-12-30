package gov.nist.drmf.interpreter.generic.exceptions;

import gov.nist.drmf.interpreter.generic.mlp.pojo.SemanticEnhancedAnnotationStatus;

/**
 * @author Andre Greiner-Petter
 */
public class MinimumRequirementNotFulfilledException extends RuntimeException {
    public MinimumRequirementNotFulfilledException() {
        super("The document does not fulfill the required minimum semantic information.");
    }

    public MinimumRequirementNotFulfilledException(SemanticEnhancedAnnotationStatus requirement) {
        super("The document does not fulfill the required minimum semantic information. Required a "
                + requirement.getDescription());
    }

    public MinimumRequirementNotFulfilledException(SemanticEnhancedAnnotationStatus requirement, SemanticEnhancedAnnotationStatus actualStatus) {
        super(
                "The document only has the semantic state of a " + actualStatus.getDescription() + ", but requires a " + requirement.getDescription()
        );
    }
}
