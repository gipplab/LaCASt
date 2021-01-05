package gov.nist.drmf.interpreter.generic.mlp;

import com.formulasearchengine.mathosphere.mlp.pojos.MathTag;
import com.formulasearchengine.mathosphere.mlp.pojos.Relation;
import com.formulasearchengine.mathosphere.mlp.text.WikiTextUtils;
import gov.nist.drmf.interpreter.common.cas.ICASEngineSymbolicEvaluator;
import gov.nist.drmf.interpreter.common.eval.INumericTestCalculator;
import gov.nist.drmf.interpreter.common.pojo.CASResult;
import gov.nist.drmf.interpreter.generic.common.GenericReplacementTool;
import gov.nist.drmf.interpreter.generic.elasticsearch.DLMFElasticSearchClient;
import gov.nist.drmf.interpreter.generic.elasticsearch.MacroResult;
import gov.nist.drmf.interpreter.generic.exceptions.MinimumRequirementNotFulfilledException;
import gov.nist.drmf.interpreter.generic.interfaces.IPartialEnhancer;
import gov.nist.drmf.interpreter.generic.macro.*;
import gov.nist.drmf.interpreter.generic.mlp.pojo.*;
import gov.nist.drmf.interpreter.common.pojo.FormulaDefinition;
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
public class SemanticEnhancer implements IPartialEnhancer {
    private static final Logger LOG = LogManager.getLogger(SemanticEnhancer.class.getName());

    private final MacroRetriever retriever;

    public SemanticEnhancer() {
        this.retriever = new MacroRetriever();
    }

    @Override
    public MOIPresentations generateAnnotatedLatex(String latex, MLPDependencyGraph graph) throws ParseException {
        MathTag mathTag = new MathTag(latex, WikiTextUtils.MathMarkUpType.LATEX);
        MOINode<MOIAnnotation> node = graph.addFormulaNode( mathTag );
        return new MOIPresentations( node );
    }

    @Override
    public void appendSemanticLatex(MOIPresentations moi, MOINode<MOIAnnotation> node) throws ParseException {
        String originalTex = node.getNode().getOriginalLaTeX();
        LOG.info("Start semantically enhancing moi " + node.getId() + ": " + originalTex);

        RetrievedMacros retrievedMacros = retriever.retrieveReplacements(node);
        coreSemanticallyEnhance(moi, node, retrievedMacros);
    }

    @Override
    public CASResult computeNumerically(String semanticLatex, CASResult casResult, INumericTestCalculator<?> numericTestCalculator) {
        // first: get instance of Maple and Mathematica as translators (only if available)
        // second: build NumericalTest (which should be the same for all)
        // third: call performTest
        // fourth: analyze test case, remove correct answers
        // fifth: update CASResult



        return null;
    }

    @Override
    public CASResult computeSemantically(String semanticLatex, CASResult casResult, ICASEngineSymbolicEvaluator<?> symbolicEvaluator) {
        return null;
    }

    private void coreSemanticallyEnhance(MOIPresentations moiPresentation, MOINode<MOIAnnotation> node, RetrievedMacros retrievedMacros) throws ParseException {
        MathematicalObjectOfInterest moi = node.getNode();
        PrintablePomTaggedExpression pte = moi.getMoi();
        Set<String> replacementPerformed = new HashSet<>();
        LOG.debug("Start replacements on MOI: " + pte.getTexString());

        GenericReplacementTool genericReplacementTool = new GenericReplacementTool(pte);
        pte = genericReplacementTool.getSemanticallyEnhancedExpression();
        LOG.debug("Replaced general patterns: " + pte.getTexString());

        List<SemanticReplacementRule> macroPatterns = retrievedMacros.getPatterns();
        int counter = 0;
        double score = 0.0;
        for( SemanticReplacementRule semanticReplacementRule : macroPatterns ) {
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
                score += semanticReplacementRule.getScore();
            }
        }

        score = counter > 0 ? score/(double)counter : 0;

        LOG.info("Semantically enhanced MOI.\n" +
                "From: "+node.getNode().getOriginalLaTeX()+"\n" +
                "To:   "+pte.getTexString()
        );

        moiPresentation.setScore(score);
        moiPresentation.setSemanticLatex(pte.getTexString());
    }
}
