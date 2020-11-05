package gov.nist.drmf.interpreter.generic.pojo;

import gov.nist.drmf.interpreter.generic.mlp.struct.MOIAnnotation;
import gov.nist.drmf.interpreter.pom.moi.MOINode;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Andre Greiner-Petter
 */
public class MOIPresentations {

    private final String genericLatex;
    private String semanticLatex;
    private final Map<String, String> casRepresentations;

    public MOIPresentations(String genericLatex) {
        this.genericLatex = genericLatex;
        this.semanticLatex = "";
        this.casRepresentations = new HashMap<>();
    }

    public MOIPresentations(MOINode<MOIAnnotation> node) {
        this(node.getNode().getOriginalLaTeX());

    }

    public MOIPresentations setSemanticLatex(String semanticLatex) {
        this.semanticLatex = semanticLatex;
        return this;
    }

    public MOIPresentations addCASPresentation(String cas, String presentation) {
        this.casRepresentations.put(cas, presentation);
        return this;
    }

    public String getGenericLatex() {
        return genericLatex;
    }

    public String getSemanticLatex() {
        return semanticLatex;
    }

    public String getCasRepresentation(String cas) {
        return casRepresentations.get(cas);
    }
}
