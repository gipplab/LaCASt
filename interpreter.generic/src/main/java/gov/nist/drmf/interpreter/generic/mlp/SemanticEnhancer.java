package gov.nist.drmf.interpreter.generic.mlp;

import com.formulasearchengine.mathosphere.mlp.pojos.Relation;
import gov.nist.drmf.interpreter.generic.common.GenericReplacementTool;
import gov.nist.drmf.interpreter.generic.elasticsearch.ElasticSearchConnector;
import gov.nist.drmf.interpreter.generic.elasticsearch.MacroResult;
import gov.nist.drmf.interpreter.generic.macro.MacroBean;
import gov.nist.drmf.interpreter.generic.macro.MacroGenericSemanticEntry;
import gov.nist.drmf.interpreter.generic.macro.MacroHelper;
import gov.nist.drmf.interpreter.generic.mlp.struct.MOIAnnotation;
import gov.nist.drmf.interpreter.pom.extensions.*;
import gov.nist.drmf.interpreter.pom.moi.MOINode;
import gov.nist.drmf.interpreter.pom.moi.MathematicalObjectOfInterest;
import mlp.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Andre Greiner-Petter
 */
public class SemanticEnhancer {
    private static final Logger LOG = LogManager.getLogger(SemanticEnhancer.class.getName());

    private ElasticSearchConnector elasticSearchConnector;

    private int considerNumberOfTopRelations = 3;
    private int considerNumberOfTopMacros = 5;

    private LinkedList<MacroBean> macroPatterns;

    private List<String> definiens;
    private List<String> macros;

    private double score;
    private int counter = 1;

    public SemanticEnhancer() {
        this.elasticSearchConnector = ElasticSearchConnector.getDefaultInstance();
        this.macroPatterns = new LinkedList<>();
        this.definiens = new LinkedList<>();
        this.macros = new LinkedList<>();
        this.score = 0;
    }

    public double getScore() {
        return score;
    }

    public List<String> getUsedDefiniens() {
        return definiens;
    }

    public List<String> getUsedMacros() {
        return macros;
    }

    public PrintablePomTaggedExpression semanticallyEnhance(MOINode<MOIAnnotation> node) throws IOException, ParseException {
        String originalTex = node.getNode().getOriginalLaTeX();
        LOG.info("Start semantically enhancing moi " + node.getId() + ": " + originalTex);

        List<MOINode<MOIAnnotation>> dependentNodes = node.getDependencyNodes();
        counter = 0;
        retrieveReplacementListsEnhance(node, dependentNodes);
        this.score = this.score / counter;
        LOG.info("Retrieved "+ macroPatterns.size() +" replacement rules. Start applying each rule.");

        MathematicalObjectOfInterest moi = node.getNode();
        PrintablePomTaggedExpression pte = moi.getMoi();
        Set<String> replacementPerformed = new HashSet<>();
        LOG.debug("Start replacements on MOI: " + pte.getTexString());

        while( !macroPatterns.isEmpty() ) {
            MacroBean macro = macroPatterns.removeFirst();
            MatcherConfig config = MacroHelper.getMatchingConfig(macro, node);

//            LinkedList<String> genericLaTeXPatterns = macro.getGenericLatex();
//            LinkedList<String> semanticLaTeXPatterns = macro.getSemanticLaTeX();
            LinkedList<MacroGenericSemanticEntry> patterns = new LinkedList<>(macro.getTex());

            while ( !patterns.isEmpty() ) {
                MacroGenericSemanticEntry entry = patterns.removeFirst();
                String genericLaTeXPattern = entry.getGenericTex();
                String semanticLaTeXPattern = entry.getSemanticTex();

                // if rule was already applied, we skip it
                if ( replacementPerformed.contains(genericLaTeXPattern) ) continue;
                else replacementPerformed.add(genericLaTeXPattern);

                LOG.debug("Apply replacement from '"+genericLaTeXPattern+"' to '"+semanticLaTeXPattern+"'.");

                MatchablePomTaggedExpression genericPattern =
                        PomMatcherBuilder.compile(genericLaTeXPattern, MacroHelper.WILDCARD_PATTERNS);
                PomMatcher matcher = genericPattern.matcher(pte, config);
                pte = matcher.replacePattern(semanticLaTeXPattern);
                LOG.debug("Replacement applied, updated MOI: " + pte.getTexString());
            }
        }

        LOG.info("Semantically enhanced MOI.\n" +
                "From: "+node.getNode().getOriginalLaTeX()+"\n" +
                "To:   "+pte.getTexString()
        );

        GenericReplacementTool genericReplacementTool = new GenericReplacementTool(pte);
        return genericReplacementTool.getSemanticallyEnhancedExpression();
    }

    private void retrieveReplacementListsEnhance(
            MOINode<MOIAnnotation> node,
            List<MOINode<MOIAnnotation>> dependencyList
    ) throws IOException {
        List<Relation> definiensList = node.getAnnotation().getAttachedRelations();
        LOG.debug("Retrieve " + definiensList.size() + " definiens for node "+ node.getId() +": " + node.getNode().getOriginalLaTeX());
        Collections.sort(definiensList);

        for ( int i = 0; i < definiensList.size() && i < considerNumberOfTopRelations; i++ ) {
            Relation definitionRelation = definiensList.get(i);
            double definiensScore = definitionRelation.getScore();
            String definition = definitionRelation.getDefinition();
            this.definiens.add(definition);
            LinkedList<MacroResult> macros = elasticSearchConnector.searchMacroDescription(definition);
            LOG.debug("For definition " + definition + ": retrieved " + macros.size() + " semantic macros " + macros);

            double maxMacroScore = macros.isEmpty() ? 0 : macros.get(0).getScore();
            for ( int j = 0; j < macros.size() && j < considerNumberOfTopMacros; j++ ) {
                MacroBean macro = macros.get(j).getMacro();
                if ( !macroPatterns.contains(macro) ) {
                    LOG.debug("Add semantic macro " + macro.getName());
                    macroPatterns.add(macro);
                    this.macros.add(macro.getName());

                    double macroScore = macros.get(j).getScore();
                    this.counter++;
                    this.score += (definiensScore+(macroScore/maxMacroScore))/2;
                }
            }
        }

        if ( !dependencyList.isEmpty() )
            retrieveReplacementListsEnhance(dependencyList.remove(0), dependencyList);
    }
}
