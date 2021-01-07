package gov.nist.drmf.interpreter.generic.mlp;

import com.formulasearchengine.mathosphere.mlp.pojos.MathTag;
import com.formulasearchengine.mathosphere.mlp.text.WikiTextUtils;
import gov.nist.drmf.interpreter.common.TranslationInformation;
import gov.nist.drmf.interpreter.common.cas.ICASEngineNumericalEvaluator;
import gov.nist.drmf.interpreter.common.eval.DefaultNumericalTestCaseBuilder;
import gov.nist.drmf.interpreter.common.eval.NativeComputerAlgebraInterfaceBuilder;
import gov.nist.drmf.interpreter.common.eval.NumericalTest;
import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import gov.nist.drmf.interpreter.common.exceptions.MinimumRequirementNotFulfilledException;
import gov.nist.drmf.interpreter.common.interfaces.IConstraintTranslator;
import gov.nist.drmf.interpreter.common.pojo.CASResult;
import gov.nist.drmf.interpreter.common.pojo.NumericResult;
import gov.nist.drmf.interpreter.common.pojo.SemanticEnhancedAnnotationStatus;
import gov.nist.drmf.interpreter.common.pojo.SymbolicCalculation;
import gov.nist.drmf.interpreter.generic.common.GenericReplacementTool;
import gov.nist.drmf.interpreter.generic.interfaces.IPartialEnhancer;
import gov.nist.drmf.interpreter.generic.macro.*;
import gov.nist.drmf.interpreter.generic.mlp.cas.CASConnections;
import gov.nist.drmf.interpreter.generic.mlp.cas.CASTranslators;
import gov.nist.drmf.interpreter.generic.mlp.pojo.MLPDependencyGraph;
import gov.nist.drmf.interpreter.generic.mlp.pojo.MOIAnnotation;
import gov.nist.drmf.interpreter.generic.mlp.pojo.MOIPresentations;
import gov.nist.drmf.interpreter.generic.mlp.pojo.SemanticReplacementRule;
import gov.nist.drmf.interpreter.pom.common.DefaultNumericTestCase;
import gov.nist.drmf.interpreter.pom.extensions.*;
import gov.nist.drmf.interpreter.pom.moi.MOINode;
import gov.nist.drmf.interpreter.pom.moi.MathematicalObjectOfInterest;
import mlp.ParseException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * @author Andre Greiner-Petter
 */
public class SemanticEnhancer implements IPartialEnhancer {
    private static final Logger LOG = LogManager.getLogger(SemanticEnhancer.class.getName());

    private final MacroRetriever retriever;

    private final CASTranslators casTranslators;

    private final CASConnections casConnections;

    public SemanticEnhancer() {
        this.retriever = new MacroRetriever();
        this.casConnections = CASConnections.getInstance();
        this.casTranslators = casConnections.getTranslators();
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
    public void appendComputationResults(MOIPresentations moi) throws MinimumRequirementNotFulfilledException {
        moi.requires( SemanticEnhancedAnnotationStatus.TRANSLATED );
        LOG.info("Compute MOI " + moi.getId());

        Map<String, CASResult> casResultMap = moi.getCasRepresentations();
        String semanticLaTeX = moi.getSemanticLatex();

        Instant start = Instant.now();
        for ( String casName : casResultMap.keySet() ) {
            LOG.info("Compute numeric and symbolic verification for CAS " + casName + " on MOI " + moi.getId());
            CASResult casResult = casResultMap.get(casName);
            LOG.debug("Compute numerical verification tests on " + moi.getId());
            NumericResult nr = computeNumerically(semanticLaTeX, casName);
            casResult.setNumericResults(nr);
            LOG.debug("Compute symbolical verification tests on " + moi.getId());
            List<SymbolicCalculation> sr = computeSymbolically(semanticLaTeX, casName);
            casResult.addSymbolicResult(sr);
        }
        Duration elapsed = Duration.between(start, Instant.now());
        LOG.printf(Level.INFO,
                "Finished numeric calculations for %s [%d.%ds]",
                moi.getId(),
                elapsed.toSecondsPart(),
                elapsed.toMillisPart()
        );
    }

    @Override
    public NumericResult computeNumerically(String semanticLatex, String casName) {
        NativeComputerAlgebraInterfaceBuilder<?> cas = this.casConnections.getCASConnection(casName);
        try {
            if ( cas == null ) {
                LOG.debug("The requested CAS is not connected with valid native CAS. Skip it.");
            } else return computeNumericResults(semanticLatex, cas);
        } catch (ComputerAlgebraSystemEngineException e) {
            LOG.warn("Unable to perform numerical tests for " + casName + " because we were unable to" +
                    "to setup the forward DLMF translator.", e);
        }
        return null;
    }

    @Override
    public List<SymbolicCalculation> computeSymbolically(String semanticLatex, String casName) {
        return new LinkedList<>();
    }

    private <T> NumericResult computeNumericResults(String semanticLatex, NativeComputerAlgebraInterfaceBuilder<T> cas) throws ComputerAlgebraSystemEngineException {
        IConstraintTranslator dlmfTranslator = this.casTranslators.getTranslator(cas.getLanguageKey());
        TranslationInformation ti = dlmfTranslator.translateToObject(semanticLatex);
        DefaultNumericalTestCaseBuilder testCaseBuilder = new DefaultNumericalTestCaseBuilder(
                cas.getNumericEvaluator(), dlmfTranslator, cas.getEvaluationScriptHandler()
        );
        DefaultNumericTestCase defaultNumericTestCase = new DefaultNumericTestCase(ti);

        List<NumericalTest> tests = testCaseBuilder.buildTestCases(ti, defaultNumericTestCase);
        ICASEngineNumericalEvaluator<T> numericEvaluator = cas.getNumericEvaluator();

        NumericResult numericResult = new NumericResult();
        for ( NumericalTest test : tests ) {
            try {
                numericEvaluator.addRequiredPackages( ti.getRequiredPackages() );
                T testResult = numericEvaluator.performNumericalTest(test);
                NumericResult partialResult = numericEvaluator.getNumericResult(testResult);
                numericResult.addFurtherResults(partialResult);
            } catch (ComputerAlgebraSystemEngineException e) {
                LOG.warn("A numeric test failed: " + e.getMessage());
            }
        }
        return numericResult;
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
