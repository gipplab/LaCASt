package gov.nist.drmf.interpreter.generic.mlp.pojo;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.formulasearchengine.mathosphere.mlp.pojos.Position;
import com.wolfram.jlink.Expr;
import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import gov.nist.drmf.interpreter.common.exceptions.InitTranslatorException;
import gov.nist.drmf.interpreter.common.pojo.*;
import gov.nist.drmf.interpreter.generic.mlp.SemanticEnhancer;
import gov.nist.drmf.interpreter.mathematica.config.MathematicaConfig;
import gov.nist.drmf.interpreter.mathematica.extension.MathematicaSimplifier;
import gov.nist.drmf.interpreter.pom.extensions.PrintablePomTaggedExpression;
import gov.nist.drmf.interpreter.pom.moi.MOIDependency;
import gov.nist.drmf.interpreter.pom.moi.MOINode;
import gov.nist.drmf.interpreter.pom.moi.MathematicalObjectOfInterest;
import mlp.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Andre Greiner-Petter
 */
@JsonPropertyOrder({
        "formula", "semanticFormula", "confidence", "translations",
        "positions", "includes", "isPartOf", "definiens"
})
public class MOIPresentations {
    private static final Logger LOG = LogManager.getLogger(MOIPresentations.class.getName());

    @JsonProperty("definiens")
    private List<FormulaDefinition> definiens;

    // for debug reasons
    @JsonIgnore
    private List<String> macros;

    @JsonProperty("formula")
    private final String genericLatex;

    @JsonProperty("semanticFormula")
    private String semanticLatex;

    @JsonProperty("translations")
    private final Map<String, CASResult> casRepresentations;

    @JsonProperty("confidence")
    private double score = 0;

    @JsonProperty("positions")
    private List<Position> positions;

    @JsonProperty("includes")
    private List<String> ingoingNodes;

    @JsonProperty("isPartOf")
    private List<String> outgoingNodes;

    private MOIPresentations() {
        genericLatex = "";
        casRepresentations = new HashMap<>();
    }

    public MOIPresentations(MOINode<MOIAnnotation> node) {
        this.genericLatex = node.getNode().getOriginalLaTeX();
        this.ingoingNodes = getDependants(node, true);
        this.outgoingNodes = getDependants(node, false);
        this.positions = node.getAnnotation().getFormula().getPositions();
        this.positions.sort( Position.getComparator() );
        LOG.debug("Setup MOI representations on graph node: " + genericLatex);
        SemanticEnhancer enhancer = new SemanticEnhancer();
        casRepresentations = new HashMap<>();
        try {
            PrintablePomTaggedExpression semanticPTE = enhancer.semanticallyEnhance(node);
            this.score = enhancer.getScore();
            this.definiens = enhancer.getUsedDefiniens();
            this.macros = enhancer.getUsedMacros();

            definiens.sort(Comparator.comparingDouble(FormulaDefinition::getScore).reversed());
            if ( semanticPTE == null ) {
                LOG.warn("Unable to semantically enhance latex.");
                return;
            }

            this.semanticLatex = semanticPTE.getTexString();
            LOG.debug("Semantically enhanced generic LaTeX: " + semanticLatex);

            CASTranslators translators = CASTranslators.getTranslatorsInstance();
            LOG.debug("Translate to all supported CAS.");
            for ( String cas : translators.getSupportedCAS() ) {
                try {
                    String translation = translators.translate(cas, semanticPTE);
                    CASResult result =  new CASResult(translation);
                    if ( cas.equals("Mathematica") ) this.tryMathematicaComputation(result);
                    this.casRepresentations.put(cas, result);
                    LOG.debug("Translation to " + cas + ": " + translation);
                } catch ( Exception e ) {
                    LOG.warn("Unable to translate expression to CAS " + cas + ": " + e.toString());
                }
            }
        } catch (IOException e) {
            LOG.warn("Unable to generate semantic LaTeX.");
            LOG.debug("Error when generating semantic LaTeX", e);
        } catch (ParseException e) {
            LOG.warn("Unable to parse LaTeX.");
            LOG.debug("Error when parsing LaTeX", e);
        } catch (InitTranslatorException e) {
            LOG.error("Unable to setup CAS translators.", e);
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
        Stream<MOIDependency<MOIAnnotation>> stream = ingoing ?
                node.getIngoingDependencies().stream() :
                node.getOutgoingDependencies().stream();
        return stream.map( d -> ingoing ? d.getSource() : d.getSink() )
                .map( MOINode::getNode )
                .map( MathematicalObjectOfInterest::getOriginalLaTeX )
                .collect(Collectors.toList());
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

    @JsonGetter("confidence")
    public double getScore() {
        return score;
    }

    @JsonGetter("definiens")
    public List<FormulaDefinition> getDefiniens() {
        return definiens;
    }

    @JsonIgnore
    public List<String> getMacros() {
        return macros;
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

    @Override
    @JsonIgnore
    public String toString() {
        String out =
                "Score: " + score + "; Used Definiens: " + definiens + "; Used Macros: " + macros + "\n" +
                " Generic LaTeX: " + genericLatex + "\n" +
                "Semantic LaTeX: " + semanticLatex + "\n";
        for ( Map.Entry<String, CASResult> trans : this.casRepresentations.entrySet() ) {
            out += String.format("%14s: %s\n", trans.getKey(), trans.getValue().getCasRepresentation());
        }
        return out;
    }
}
