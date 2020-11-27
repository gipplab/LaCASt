package gov.nist.drmf.interpreter.generic.mlp.struct;

import gov.nist.drmf.interpreter.common.exceptions.InitTranslatorException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.generic.mlp.SemanticEnhancer;
import gov.nist.drmf.interpreter.pom.extensions.PrintablePomTaggedExpression;
import gov.nist.drmf.interpreter.pom.moi.MOINode;
import mlp.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author Andre Greiner-Petter
 */
public class MOIPresentations {
    private static final Logger LOG = LogManager.getLogger(MOIPresentations.class.getName());

    private List<String> definiens;
    private List<String> macros;

    private final String genericLatex;
    private String semanticLatex;
    private final Map<String, String> casRepresentations;

    private double score = 0;

    public MOIPresentations(MOINode<MOIAnnotation> node) {
        this.genericLatex = node.getNode().getOriginalLaTeX();
        LOG.debug("Setup MOI representations on graph node: " + genericLatex);
        SemanticEnhancer enhancer = new SemanticEnhancer();
        casRepresentations = new HashMap<>();
        try {
            PrintablePomTaggedExpression semanticPTE = enhancer.semanticallyEnhance(node);
            this.score = enhancer.getScore();
            this.definiens = enhancer.getUsedDefiniens();
            this.macros = enhancer.getUsedMacros();
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
                } catch ( TranslationException te ) {
                    LOG.warn("Unable to translate expression to CAS " + cas);
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

    public String getGenericLatex() {
        return genericLatex;
    }

    public String getSemanticLatex() {
        return semanticLatex;
    }

    public String getCasRepresentation(String cas) {
        return casRepresentations.get(cas);
    }

    @Override
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
