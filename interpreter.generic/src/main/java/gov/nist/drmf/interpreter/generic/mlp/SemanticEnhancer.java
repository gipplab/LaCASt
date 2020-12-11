package gov.nist.drmf.interpreter.generic.mlp;

import com.formulasearchengine.mathosphere.mlp.pojos.Relation;
import gov.nist.drmf.interpreter.generic.common.GenericReplacementTool;
import gov.nist.drmf.interpreter.generic.elasticsearch.DLMFElasticSearchClient;
import gov.nist.drmf.interpreter.generic.elasticsearch.MacroResult;
import gov.nist.drmf.interpreter.generic.macro.*;
import gov.nist.drmf.interpreter.generic.mlp.pojo.MOIAnnotation;
import gov.nist.drmf.interpreter.generic.mlp.pojo.MLPLacastScorer;
import gov.nist.drmf.interpreter.common.pojo.FormulaDefinition;
import gov.nist.drmf.interpreter.generic.mlp.pojo.SemanticReplacementRule;
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

    private final MacroDistributionAnalyzer macroDist;

    private DLMFElasticSearchClient elasticSearchConnector;

    private static final int considerNumberOfTopRelations = 3;
    private static final int considerNumberOfTopMacros = 5;

    private final Set<String> macroPatternMemory;
    private final LinkedList<SemanticReplacementRule> macroPatterns;

    private final Set<String> definiensMemory;
    private final List<FormulaDefinition> definiens;
    private final List<String> macros;

    private double score;

    public SemanticEnhancer() {
        this.macroPatternMemory = new HashSet<>();
        this.macroPatterns = new LinkedList<>();
        this.definiensMemory = new HashSet<>();
        this.definiens = new LinkedList<>();
        this.macros = new LinkedList<>();
        this.score = 0;
        this.macroDist = MacroDistributionAnalyzer.getStandardInstance();
    }

    private void reset() {
        elasticSearchConnector = new DLMFElasticSearchClient();
        macroPatternMemory.clear();
        macroPatterns.clear();
        definiensMemory.clear();
        definiens.clear();
        macros.clear();
        score = 0;
    }

    private void cleanup() {
        elasticSearchConnector.stop();
        elasticSearchConnector = null;
    }

    public double getScore() {
        return score;
    }

    public List<FormulaDefinition> getUsedDefiniens() {
        return definiens;
    }

    public List<String> getUsedMacros() {
        return macros;
    }

    public PrintablePomTaggedExpression semanticallyEnhance(MOINode<MOIAnnotation> node) throws IOException, ParseException {
        reset();
        PrintablePomTaggedExpression result = coreSemanticallyEnhance(node);
        cleanup();
        return result;
    }

    private PrintablePomTaggedExpression coreSemanticallyEnhance(MOINode<MOIAnnotation> node) throws IOException, ParseException {
        String originalTex = node.getNode().getOriginalLaTeX();
        LOG.info("Start semantically enhancing moi " + node.getId() + ": " + originalTex);

        definiens.addAll(
                node.getAnnotation().getAttachedRelations().stream()
                    .map( r -> new FormulaDefinition(r.getScore(), r.getDefinition()) )
                    .collect(Collectors.toList())
        );

        List<MOINode<MOIAnnotation>> dependentNodes = node.getDependencyNodes();
        retrieveReplacementListsEnhance(node, dependentNodes);
        LOG.info("Retrieved "+ macroPatterns.size() +" replacement rules. Start applying each rule.");

        MathematicalObjectOfInterest moi = node.getNode();
        PrintablePomTaggedExpression pte = moi.getMoi();
        Set<String> replacementPerformed = new HashSet<>();
        LOG.debug("Start replacements on MOI: " + pte.getTexString());

        GenericReplacementTool genericReplacementTool = new GenericReplacementTool(pte);
        pte = genericReplacementTool.getSemanticallyEnhancedExpression();
        LOG.debug("Replaced general patterns: " + pte.getTexString());

        macroPatterns.sort((a, b) -> {
            double diff = a.getScore() - b.getScore();
            if ( diff == 0 ) {
                MacroCounter c1 = macroDist.getMacroCounter( "\\" + a.getMacro().getName() );
                MacroCounter c2 = macroDist.getMacroCounter( "\\" + b.getMacro().getName() );

                return c2.getMacroCounter() - c1.getMacroCounter();
            }

            return Double.compare(b.getScore(), a.getScore());
        });

        int counter = 0;
        while( !macroPatterns.isEmpty() ) {
            SemanticReplacementRule semanticReplacementRule = macroPatterns.removeFirst();
            MacroBean macro = semanticReplacementRule.getMacro();
            MatcherConfig config = MacroHelper.getMatchingConfig(macro, node);

            MacroGenericSemanticEntry entry = semanticReplacementRule.getPattern();
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
            if ( matcher.performedReplacements() ) {
                counter++;
                this.score += semanticReplacementRule.getScore();
            }
        }

        this.score = counter > 0 ? score/(double)counter : 0;

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
        Collections.sort(definiensList);

        for ( int i = 0; i < definiensList.size() && i < considerNumberOfTopRelations; i++ ) {
            Relation definitionRelation = definiensList.get(i);
            double definiensScore = definitionRelation.getScore();
            String definition = definitionRelation.getDefinition();
            if ( this.definiensMemory.contains(definition) ) continue;

//            this.definiens.add(new FormulaDefiniens(definiensScore, definition));
            LinkedList<MacroResult> macros = elasticSearchConnector.searchMacroDescription(definition);
            LOG.debug("For definition " + definition + ": retrieved " + macros.size() + " semantic macros " + macros);

            double maxMacroScore = macros.isEmpty() ? 0 : macros.get(0).getScore();
            MLPLacastScorer scorer = new MLPLacastScorer(maxMacroScore);

            for ( int j = 0; j < macros.size() && j < considerNumberOfTopMacros; j++ ) {
                MacroResult macroResult = macros.get(j);
                MacroBean macro = macroResult.getMacro();
                if ( this.macros.contains(macro.getName()) )
                    continue;

                this.macros.add( macro.getName() );
                if ( !macroPatternMemory.contains(macro.getName()) ) {
                    LOG.debug("Add semantic macro " + macro.getName());
                    macroPatternMemory.add(macro.getName());

                    MacroCounter counter = macroDist.getMacroCounter("\\" + macro.getName());
                    for ( MacroGenericSemanticEntry entry : macro.getTex() ) {
                        double score = counter != null ?
                                scorer.getScore( definiensScore, macroResult.getScore(), entry.getScore() ) :
                                0;
                        macroPatterns.add( new SemanticReplacementRule(macro, entry, score) );
                    }
                }
            }
        }

        if ( !dependencyList.isEmpty() )
            retrieveReplacementListsEnhance(dependencyList.remove(0), dependencyList);
    }
}
