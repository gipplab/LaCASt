package gov.nist.drmf.interpreter.generic.mlp.struct;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import gov.nist.drmf.interpreter.common.exceptions.InitTranslatorException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.generic.mlp.SemanticEnhancer;
import gov.nist.drmf.interpreter.generic.pojo.FormulaDefiniens;
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
        "formula", "semanticFormula", "confidence", "translations", "includes", "isPartOf", "definiens"
})
public class MOIPresentations {
    private static final Logger LOG = LogManager.getLogger(MOIPresentations.class.getName());

    @JsonProperty("definiens")
    private List<FormulaDefiniens> definiens;

    // for debug reasons
    @JsonIgnore
    private List<String> macros;

    @JsonProperty("formula")
    private final String genericLatex;

    @JsonProperty("semanticFormula")
    private String semanticLatex;

    @JsonProperty("translations")
    private final Map<String, String> casRepresentations;

    @JsonProperty("confidence")
    private double score = 0;

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
        LOG.debug("Setup MOI representations on graph node: " + genericLatex);
        SemanticEnhancer enhancer = new SemanticEnhancer();
        casRepresentations = new HashMap<>();
        try {
            PrintablePomTaggedExpression semanticPTE = enhancer.semanticallyEnhance(node);
            this.score = enhancer.getScore();
            this.definiens = enhancer.getUsedDefiniens();
            this.macros = enhancer.getUsedMacros();

            definiens.sort(Comparator.comparingDouble(FormulaDefiniens::getScore).reversed());

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
                    this.casRepresentations.put(cas, translation);
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
    public String getCasRepresentation(String cas) {
        return casRepresentations.get(cas);
    }

    @JsonGetter("translations")
    public Map<String, String> getCasRepresentations() {
        return casRepresentations;
    }

    @JsonGetter("confidence")
    public double getScore() {
        return score;
    }

    @JsonGetter("definiens")
    public List<FormulaDefiniens> getDefiniens() {
        return definiens;
    }

    @JsonIgnore
    public List<String> getMacros() {
        return macros;
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
        for ( Map.Entry<String, String> trans : this.casRepresentations.entrySet() ) {
            out += String.format("%14s: %s\n", trans.getKey(), trans.getValue());
        }
        return out;
    }
}
