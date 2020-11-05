package gov.nist.drmf.interpreter.generic.mlp;

import gov.nist.drmf.interpreter.generic.mlp.struct.MOIAnnotation;
import gov.nist.drmf.interpreter.pom.moi.MOINode;
import gov.nist.drmf.interpreter.pom.moi.MathematicalObjectOfInterest;

import java.util.List;

/**
 * @author Andre Greiner-Petter
 */
public class SemanticEnhancer {
    public SemanticEnhancer() {

    }

    public String semanticallyEnhance(MOINode<MOIAnnotation> node) {
        List<MOINode<MOIAnnotation>> dependentNodes = node.getDependencyNodes();
        dependentNodes.add(0, node);

        MathematicalObjectOfInterest moi = node.getNode();
        /*
        TODO search for dlmf-macros (descriptions)
        TODO perform replacements
         */
        return null;
    }
}
