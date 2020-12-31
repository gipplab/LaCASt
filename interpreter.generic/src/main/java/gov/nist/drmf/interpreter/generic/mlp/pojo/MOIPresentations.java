package gov.nist.drmf.interpreter.generic.mlp.pojo;

import com.fasterxml.jackson.annotation.*;
import com.formulasearchengine.mathosphere.mlp.pojos.Position;
import com.wolfram.jlink.Expr;
import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import gov.nist.drmf.interpreter.common.exceptions.InitTranslatorException;
import gov.nist.drmf.interpreter.common.pojo.*;
import gov.nist.drmf.interpreter.generic.mlp.SemanticEnhancer;
import gov.nist.drmf.interpreter.mathematica.config.MathematicaConfig;
import gov.nist.drmf.interpreter.mathematica.extension.MathematicaSimplifier;
import gov.nist.drmf.interpreter.pom.extensions.PrintablePomTaggedExpression;
import gov.nist.drmf.interpreter.pom.moi.INode;
import gov.nist.drmf.interpreter.pom.moi.MOIDependency;
import gov.nist.drmf.interpreter.pom.moi.MOINode;
import gov.nist.drmf.interpreter.pom.moi.MathematicalObjectOfInterest;
import mlp.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Andre Greiner-Petter
 */
@JsonPropertyOrder({
        "id", "formula", "semanticFormula", "confidence", "translations",
        "positions", "includes", "isPartOf", "definiens"
})
public class MOIPresentations {
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

    private void tryMathematicaComputation(CASResult casResult) {
        if ( !MathematicaConfig.isMathematicaPresent() ) return;

        // let's add a fake numeric computation just to show how it may look like
        NumericResult nr = new NumericResult(false, 0, 0, 0);
        NumericCalculation nc = new NumericCalculation();
        nc.setResult("no-computation-dummy");
        HashMap<String, String> testValues = new HashMap<>();
        testValues.put("\\alpha", "1");
        testValues.put("\\beta", "2");
        nc.setTestValues(testValues);
        nr.addTestCalculations( nc );
        casResult.setNumericResults(nr);

        MathematicaSimplifier simplifier = new MathematicaSimplifier();
        simplifier.setTimeout(1);
        try {
            Expr symbolicResultExpr = simplifier.simplify(casResult.getCasRepresentation(), null);
            SymbolicCalculation sc = new SymbolicCalculation();
            sc.setResult( symbolicResultExpr.toString() );
            sc.setTestProperty("fullsimplify");
            casResult.addSymbolicResult( sc );
        } catch (ComputerAlgebraSystemEngineException e) {
            LOG.debug("Unable to simplify mathematical expression.");
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
    public SemanticEnhancedAnnotationStatus getStatus() {
        return status;
    }

    @JsonIgnore
    public void setStatus(SemanticEnhancedAnnotationStatus status) {
        this.status = status;
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
    public double getScore() {
        return score;
    }

    @JsonSetter("confidence")
    public void setScore(double score) {
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
        this.ingoingNodes = ingoingNodes;
    }

    @JsonSetter("isPartOf")
    public void setOutgoingNodes(List<String> outgoingNodes) {
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
