package gov.nist.drmf.interpreter.generic.mlp;

import com.formulasearchengine.mathosphere.mlp.pojos.Relation;
import gov.nist.drmf.interpreter.generic.elasticsearch.ElasticSearchConnector;
import gov.nist.drmf.interpreter.generic.elasticsearch.MacroResult;
import gov.nist.drmf.interpreter.generic.macro.MacroBean;
import gov.nist.drmf.interpreter.generic.macro.MacroHelper;
import gov.nist.drmf.interpreter.generic.mlp.struct.MOIAnnotation;
import gov.nist.drmf.interpreter.pom.extensions.*;
import gov.nist.drmf.interpreter.pom.moi.MOINode;
import gov.nist.drmf.interpreter.pom.moi.MathematicalObjectOfInterest;
import mlp.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author Andre Greiner-Petter
 */
public class SemanticEnhancer {
    private static final Logger LOG = LogManager.getLogger(SemanticEnhancer.class.getName());

    private ElasticSearchConnector elasticSearchConnector;

    private int considerNumberOfTopRelations = 3;
    private int considerNumberOfTopMacros = 2;

    private LinkedList<String> genericLaTeXPatterns;
    private LinkedList<String> semanticLaTeXPatterns;

    public SemanticEnhancer() {
        this.elasticSearchConnector = ElasticSearchConnector.getDefaultInstance();
        this.genericLaTeXPatterns = new LinkedList<>();
        this.semanticLaTeXPatterns = new LinkedList<>();
    }

    public PrintablePomTaggedExpression semanticallyEnhance(MOINode<MOIAnnotation> node) throws IOException, ParseException {
        LOG.info("Start semantically enhancing moi " + node.getId() + ": " + node.getNode().getOriginalLaTeX());
        List<MOINode<MOIAnnotation>> dependentNodes = node.getDependencyNodes();
        retrieveReplacementListsEnhance(node, dependentNodes);
        LOG.info("Retrieved all replacement rules. Start applying each rule.");

        MathematicalObjectOfInterest moi = node.getNode();
        PrintablePomTaggedExpression pte = moi.getMoi();
        Set<String> replacementPerformed = new HashSet<>();
        LOG.debug("Start replacements on MOI: " + pte.getTexString());

        while( !semanticLaTeXPatterns.isEmpty() ) {
            String genericLaTeXPattern = genericLaTeXPatterns.removeFirst();
            String semanticLaTeXPattern = semanticLaTeXPatterns.removeFirst();

            // if rule was already applied, we skip it
            if ( replacementPerformed.contains(genericLaTeXPattern) ) continue;
            else replacementPerformed.add(genericLaTeXPattern);

            LOG.debug("Apply replacement from '"+genericLaTeXPattern+"' to '"+semanticLaTeXPattern+"'.");

            MatchablePomTaggedExpression genericPattern =
                    PomMatcherBuilder.compile(genericLaTeXPattern, MacroHelper.WILDCARD_PATTERNS);
            PomMatcher matcher = genericPattern.matcher(pte);
            pte = matcher.replacePattern(semanticLaTeXPattern);
            LOG.debug("Replacement applied, updated MOI: " + pte.getTexString());
        }

        LOG.info("Semantically enhanced MOI.\n" +
                "From: "+node.getNode().getOriginalLaTeX()+"\n" +
                "To:   "+pte.getTexString()
        );

        return pte;
    }

    private void retrieveReplacementListsEnhance(
            MOINode<MOIAnnotation> node,
            List<MOINode<MOIAnnotation>> dependencyList
    ) throws IOException {
        List<Relation> definiensList = node.getAnnotation().getAttachedRelations();
        LOG.debug("Retrieve " + definiensList.size() + " definiens for node "+ node.getId() +": " + node.getNode().getOriginalLaTeX());
        for ( int i = 0; i < definiensList.size() && i < considerNumberOfTopRelations; i++ ) {
            Relation definitionRelation = definiensList.get(i);
            String definition = definitionRelation.getDefinition();
            LinkedList<MacroResult> macros = elasticSearchConnector.searchMacroDescription(definition);
            LOG.debug("Retrieved semantic macros: " + macros);

            for ( int j = 0; j < macros.size() && j < considerNumberOfTopMacros; j++ ) {
                MacroBean macro = macros.get(j).getMacro();
                LinkedList<String> genericList = macro.getGenericLatex();
                LinkedList<String> semanticList = macro.getSemanticLaTeX();

                if ( genericList.size() != semanticList.size() ) {
                    LOG.warn("Retrieved an equal size of generic-semantic mapping for " + macros.get(j) + ". Skip these replacement rules");
                } else {
                    LOG.debug("Add "+semanticList.size()+" semantic replacement rules: " + semanticList);
                    this.genericLaTeXPatterns.addAll(genericList);
                    this.semanticLaTeXPatterns.addAll(semanticList);
                }
            }
        }

        if ( !dependencyList.isEmpty() )
            retrieveReplacementListsEnhance(dependencyList.remove(0), dependencyList);
    }
}
