package gov.nist.drmf.interpreter.generic.mlp.pojo;

import com.fasterxml.jackson.annotation.*;
import com.formulasearchengine.mathosphere.mlp.pojos.Position;
import gov.nist.drmf.interpreter.common.interfaces.SemanticallyRanked;
import gov.nist.drmf.interpreter.common.pojo.CASResult;
import gov.nist.drmf.interpreter.common.pojo.FormulaDefinition;
import gov.nist.drmf.interpreter.common.pojo.SemanticEnhancedAnnotationStatus;
import gov.nist.drmf.interpreter.pom.moi.INode;
import gov.nist.drmf.interpreter.pom.moi.MOINode;
import gov.nist.drmf.interpreter.pom.moi.MathematicalObjectOfInterest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Andre Greiner-Petter
 */
@JsonPropertyOrder({
        "id", "formula", "semanticFormula", "confidence", "translations",
        "positions", "includes", "isPartOf", "definiens"
})
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MOIPresentations implements SemanticallyRanked {
    private static final Logger LOG = LogManager.getLogger(MOIPresentations.class.getName());

    @JsonIgnore
    private SemanticEnhancedAnnotationStatus status = SemanticEnhancedAnnotationStatus.BASE;

    @JsonProperty("id")
    private final String id;

    @JsonProperty("formula")
    private final String genericLatex;

    @JsonProperty("definiens")
    private List<FormulaDefinition> definiens;

    @JsonProperty("semanticFormula")
    private String semanticLatex;

    @JsonProperty("translations")
    private final Map<String, CASResult> casRepresentations;

    @JsonProperty("confidence")
    private Double score;

    @JsonProperty("positions")
    private List<Position> positions;

    @JsonProperty("includes")
    private List<String> ingoingNodes;

    @JsonProperty("isPartOf")
    private List<String> outgoingNodes;

    public MOIPresentations() {
        id = "FORMULA_EMPTY";
        genericLatex = "";
        casRepresentations = new HashMap<>();
    }

    /**
     * Copy constructor
     */
    public MOIPresentations(MOIPresentations copy) {
        this.status = copy.status;
        this.id = copy.id;
        this.genericLatex = copy.genericLatex;
        if (copy.definiens != null) this.definiens = new LinkedList<>(copy.definiens);
        this.semanticLatex = copy.semanticLatex;
        this.casRepresentations = new HashMap<>(copy.casRepresentations);
        this.score = copy.score;
        if (copy.positions != null) this.positions = new LinkedList<>(copy.positions);
        if (copy.ingoingNodes != null) this.ingoingNodes = new LinkedList<>(copy.ingoingNodes);
        if (copy.outgoingNodes != null) this.outgoingNodes = new LinkedList<>(copy.outgoingNodes);
    }

    public MOIPresentations(MOINode<MOIAnnotation> node) {
        this.id = node.getId();
        this.genericLatex = node.getNode().getOriginalLaTeX();
        this.ingoingNodes = getDependants(node, true);
        this.outgoingNodes = getDependants(node, false);
        this.casRepresentations = new HashMap<>();

        if ( node.getAnnotation() != null ) {
            if ( node.getAnnotation().getFormula() != null ) {
                this.positions = node.getAnnotation().getFormula().getPositions();
                this.positions.sort( Position.getComparator() );
            }

            this.definiens = node.getAnnotation().getAttachedRelations().stream()
                    .map(r -> new FormulaDefinition(r.getScore(), r.getDefinition()))
                    .collect(Collectors.toCollection(LinkedList::new));
            if ( !definiens.isEmpty() ) status = SemanticEnhancedAnnotationStatus.SEMANTICALLY_ANNOTATED;
        }
    }

    private List<String> getDependants(MOINode<MOIAnnotation> node, boolean ingoing) {
        Collection<INode<MOIAnnotation>> nodes = ingoing ?
                node.getIngoingNodes() : node.getOutgoingNodes();

        return nodes.stream().map( n -> (MOINode<MOIAnnotation>)n )
                .map( MOINode::getNode )
                .map( MathematicalObjectOfInterest::getOriginalLaTeX )
                .collect(Collectors.toList());
    }

    @JsonIgnore
    public SemanticEnhancedAnnotationStatus getRank() {
        return status;
    }

    @JsonGetter("id")
    public String getId() {
        return id;
    }

    @JsonGetter("formula")
    public String getGenericLatex() {
        return genericLatex;
    }

    @JsonGetter("semanticFormula")
    public String getSemanticLatex() {
        return semanticLatex;
    }

    @JsonIgnore
    public CASResult getCasResults(String cas) {
        return casRepresentations.get(cas);
    }

    @JsonGetter("translations")
    public Map<String, CASResult> getCasRepresentations() {
        return casRepresentations;
    }

    @JsonSetter("translations")
    public void setCasRepresentations(Map<String, CASResult> casRepresentations) {
        casRepresentations.forEach(this::addCasRepresentation);
    }

    @JsonIgnore
    public void addCasRepresentation(String cas, CASResult casResult) {
        updateStatus(getStatusFromCASResult(casResult));
        this.casRepresentations.put(cas, casResult);
    }

    @JsonIgnore
    private SemanticEnhancedAnnotationStatus getStatusFromCASResult(CASResult result) {
        if ( result.getNumericResults() != null || result.getSymbolicResults() != null ) {
            if ( !status.hasPassed( SemanticEnhancedAnnotationStatus.COMPUTED ) )
                return SemanticEnhancedAnnotationStatus.COMPUTED;
        } else if ( !result.getCasRepresentation().isBlank() )
            return SemanticEnhancedAnnotationStatus.TRANSLATED;
        return SemanticEnhancedAnnotationStatus.BASE;
    }

    @JsonIgnore
    private void updateStatus(SemanticEnhancedAnnotationStatus newStatus) {
        if ( newStatus.hasPassed(status) ) status = newStatus;
    }

    @JsonGetter("confidence")
    public Double getScore() {
        return score;
    }

    @JsonSetter("confidence")
    public void setScore(Double score) {
        this.score = score;
    }

    @JsonGetter("definiens")
    public List<FormulaDefinition> getDefiniens() {
        return definiens;
    }

    @JsonGetter("positions")
    public List<Position> getPositions() {
        return positions;
    }

    @JsonGetter("includes")
    public List<String> getIngoingNodes() {
        return ingoingNodes;
    }

    @JsonGetter("isPartOf")
    public List<String> getOutgoingNodes() {
        return outgoingNodes;
    }

    @JsonSetter("definiens")
    public void setDefiniens(List<FormulaDefinition> definiens) {
        if ( !definiens.isEmpty() && !status.hasPassed( SemanticEnhancedAnnotationStatus.SEMANTICALLY_ANNOTATED ) )
            status = SemanticEnhancedAnnotationStatus.SEMANTICALLY_ANNOTATED;
        this.definiens = definiens;
    }

    @JsonSetter("semanticFormula")
    public void setSemanticLatex(String semanticLatex) {
        if ( !semanticLatex.isBlank() && !status.hasPassed( SemanticEnhancedAnnotationStatus.TRANSLATED ) )
            status = SemanticEnhancedAnnotationStatus.TRANSLATED;
        this.semanticLatex = semanticLatex;
    }

    @JsonSetter("includes")
    public void setIngoingNodes(List<String> ingoingNodes) {
        if ( !ingoingNodes.isEmpty() && !status.hasPassed( SemanticEnhancedAnnotationStatus.SEMANTICALLY_ANNOTATED ) )
            status = SemanticEnhancedAnnotationStatus.SEMANTICALLY_ANNOTATED;
        this.ingoingNodes = ingoingNodes;
    }

    @JsonSetter("isPartOf")
    public void setOutgoingNodes(List<String> outgoingNodes) {
        if ( !outgoingNodes.isEmpty() && !status.hasPassed( SemanticEnhancedAnnotationStatus.SEMANTICALLY_ANNOTATED ) )
            status = SemanticEnhancedAnnotationStatus.SEMANTICALLY_ANNOTATED;
        this.outgoingNodes = outgoingNodes;
    }

    @Override
    @JsonIgnore
    public String toString() {
        String out =
                "Score: " + score + "; Used Definiens: " + definiens + "; Used Macros: ---" + "\n" +
                " Generic LaTeX: " + genericLatex + "\n" +
                "Semantic LaTeX: " + semanticLatex + "\n";
        for ( Map.Entry<String, CASResult> trans : this.casRepresentations.entrySet() ) {
            out += String.format("%14s: %s\n", trans.getKey(), trans.getValue().getCasRepresentation());
        }
        return out;
    }
}
