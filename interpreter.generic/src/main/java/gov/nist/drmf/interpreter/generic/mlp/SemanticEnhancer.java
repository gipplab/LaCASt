package gov.nist.drmf.interpreter.generic.mlp;

import com.formulasearchengine.mathosphere.mlp.pojos.MathTag;
import com.formulasearchengine.mathosphere.mlp.text.WikiTextUtils;
import gov.nist.drmf.interpreter.common.TranslationInformation;
import gov.nist.drmf.interpreter.common.cas.ICASEngineNumericalEvaluator;
import gov.nist.drmf.interpreter.common.cas.ICASEngineSymbolicEvaluator;
import gov.nist.drmf.interpreter.common.config.GenericLacastConfig;
import gov.nist.drmf.interpreter.common.eval.*;
import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import gov.nist.drmf.interpreter.common.exceptions.InitTranslatorException;
import gov.nist.drmf.interpreter.common.exceptions.MinimumRequirementNotFulfilledException;
import gov.nist.drmf.interpreter.common.interfaces.IConstraintTranslator;
import gov.nist.drmf.interpreter.common.pojo.CASResult;
import gov.nist.drmf.interpreter.common.eval.NumericResult;
import gov.nist.drmf.interpreter.common.pojo.SemanticEnhancedAnnotationStatus;
import gov.nist.drmf.interpreter.common.eval.SymbolicResult;
import gov.nist.drmf.interpreter.core.api.DLMFTranslator;
import gov.nist.drmf.interpreter.generic.common.GenericConstantReplacer;
import gov.nist.drmf.interpreter.generic.common.GenericReplacementTool;
import gov.nist.drmf.interpreter.generic.interfaces.IPartialEnhancer;
import gov.nist.drmf.interpreter.generic.macro.*;
import gov.nist.drmf.interpreter.generic.mlp.cas.CASConnections;
import gov.nist.drmf.interpreter.generic.mlp.cas.CASTranslators;
import gov.nist.drmf.interpreter.generic.mlp.pojo.*;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author Andre Greiner-Petter
 */
public class SemanticEnhancer implements IPartialEnhancer {
    private static final Logger LOG = LogManager.getLogger(SemanticEnhancer.class.getName());

    private final MacroRetriever retriever;

    private final GenericLacastConfig config;

    private CASConnections casConnections;

    protected SemanticEnhancer() {
        this(GenericLacastConfig.getDefaultConfig());
    }

    public SemanticEnhancer(GenericLacastConfig config) {
        this.retriever = new MacroRetriever(config);
        this.config = config;
    }

    private synchronized void lazyInit() {
        if ( casConnections == null )
            this.casConnections = new CASConnections(config);
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
        Instant start = Instant.now();
        for ( String casName : casResultMap.keySet() ) {
            appendComputationResults(moi, casName);
        }
        Duration elapsed = Duration.between(start, Instant.now());
        LOG.printf(Level.INFO,
                "Finished all calculations for %s [%d.%ds]",
                moi.getId(),
                elapsed.toSecondsPart(),
                elapsed.toMillisPart()
        );
    }

    @Override
    public void appendComputationResults(MOIPresentations moi, String casName) throws MinimumRequirementNotFulfilledException {
        Instant start = Instant.now();
//        if ( moi.getId().equals("FORMULA_43e16b736c3ae9163cfddd4918b3c9b8") ) return;

        moi.requires( SemanticEnhancedAnnotationStatus.TRANSLATED );
        Map<String, CASResult> casResultMap = moi.getCasRepresentations();
        String semanticLaTeX = moi.getSemanticLatex();
        LOG.info("Compute numeric and symbolic verification for CAS " + casName + " on MOI " + moi.getId());
        CASResult casResult = casResultMap.get(casName);
        LOG.debug("Compute numerical verification tests on " + moi.getId());
        NumericResult nr = computeNumerically(semanticLaTeX, casName);
        casResult.setNumericResults(nr);
        LOG.debug("Compute symbolical verification tests on " + moi.getId());
        SymbolicResult sr = computeSymbolically(semanticLaTeX, casName);
        casResult.setSymbolicResults(sr);

        Duration elapsed = Duration.between(start, Instant.now());
        LOG.printf(Level.INFO,
                "Finished numeric and symbolic in %s calculations for %s [%d.%ds]",
                casName,
                moi.getId(),
                elapsed.toSecondsPart(),
                elapsed.toMillisPart()
        );
    }

    @Override
    public NumericResult computeNumerically(String semanticLatex, String casName) {
        if ( EvaluationSkipper.shouldNotBeEvaluated(semanticLatex) ) {
            LOG.debug("The test expression should not be evaluated due to missing equation or because it contains underscores (troublesome for CAS): " + semanticLatex);
            return new NumericResult().markAsSkipped();
        }

        lazyInit();
        NativeComputerAlgebraInterfaceBuilder cas = this.casConnections.getCASConnection(casName);
        try {
            if ( cas == null ) {
                LOG.debug("The requested CAS is not connected with valid native CAS. Skip it.");
            } else {
                NumericResult result = computeNumericResults(semanticLatex, cas);
                try { cas.getCASEngine().forceGC(); }
                catch (NullPointerException | ComputerAlgebraSystemEngineException e){
                    LOG.warn("Unable to call GC in CAS " + casName + ". Ignore it and hope we can survive", e);
                }
                return result;
            }
        } catch (ComputerAlgebraSystemEngineException e) {
            LOG.warn("Unable to perform numerical tests for " + casName + ": " + semanticLatex, e);
        } catch (Exception e) {
            LOG.warn("Unable to analyze test. Something went wrong: " + semanticLatex, e);
        }
        return null;
    }

    @Override
    public SymbolicResult computeSymbolically(String semanticLatex, String casName) {
        if ( EvaluationSkipper.shouldNotBeEvaluated(semanticLatex) ) {
            LOG.debug("The test expression should not be evaluated due to missing equation or because it contains underscores (troublesome for CAS): " + semanticLatex);
            return new SymbolicResult().markAsSkipped();
        }

        lazyInit();
        NativeComputerAlgebraInterfaceBuilder cas = this.casConnections.getCASConnection(casName);
        try {
            if ( cas == null ) {
                LOG.debug("The requested CAS "+ casName +" is not connected with valid native CAS. Skip it.");
            } else {
                SymbolicResult result = computeSymbolicResults(semanticLatex, cas);
                try { cas.getCASEngine().forceGC(); }
                catch (NullPointerException | ComputerAlgebraSystemEngineException e){
                    LOG.warn("Unable to call GC in CAS "+ casName +". Ignore it and hope we can survive", e);
                }
                return result;
            }
        } catch (Exception e) {
            LOG.warn("Unable to analyze test. Something went wrong: " + semanticLatex, e);
        }
        return null;
    }

    private synchronized NumericResult computeNumericResults(
            String semanticLatex,
            NativeComputerAlgebraInterfaceBuilder cas
    ) throws ComputerAlgebraSystemEngineException {
        NumericalConfig config = this.casConnections.getNumericalConfig(cas.getLanguageKey());
        IConstraintTranslator dlmfTranslator;
        try {
            dlmfTranslator = new DLMFTranslator(cas.getLanguageKey());
        } catch (InitTranslatorException e) {
            throw new ComputerAlgebraSystemEngineException(e);
        }
        TranslationInformation ti = dlmfTranslator.translateToObject(semanticLatex);
        DefaultNumericalTestCaseBuilder testCaseBuilder = new DefaultNumericalTestCaseBuilder(
                config, cas.getNumericEvaluator(), dlmfTranslator, cas.getEvaluationScriptHandler()
        );
        DefaultNumericTestCase defaultNumericTestCase = new DefaultNumericTestCase(ti);

        List<NumericalTest> tests = testCaseBuilder.buildTestCases(ti, defaultNumericTestCase);
        ICASEngineNumericalEvaluator numericEvaluator = cas.getNumericEvaluator();

        NumericResult numericResult = new NumericResult();
        for ( NumericalTest test : tests ) {
            try {
                NumericResult partialResult = numericEvaluator.performNumericTest(test);
                numericResult.addFurtherResults(partialResult);
            } catch (ComputerAlgebraSystemEngineException e) {
                LOG.warn("A numeric test failed: " + e.getMessage());
            }
        }
        return numericResult;
    }

    private synchronized SymbolicResult computeSymbolicResults(
            String semanticLatex,
            NativeComputerAlgebraInterfaceBuilder cas
    ) {
        SymbolicalConfig config = this.casConnections.getSymbolicalConfig(cas.getLanguageKey());
        IConstraintTranslator dlmfTranslator;
        try {
            dlmfTranslator = new DLMFTranslator(cas.getLanguageKey());
        } catch (InitTranslatorException e) {
            return new SymbolicResult().markAsCrashed();
        }
        ISymbolicTestCases[] testCases = cas.getDefaultSymbolicTestCases();

        SymbolicalTest symbolicalTest = new SymbolicalTest(config, dlmfTranslator, semanticLatex, testCases);
        ICASEngineSymbolicEvaluator symbolicEvaluator = cas.getSymbolicEvaluator();
        return symbolicEvaluator.performSymbolicTest(symbolicalTest);
    }

    private void coreSemanticallyEnhance(MOIPresentations moiPresentation, MOINode<MOIAnnotation> node, RetrievedMacros retrievedMacros) throws ParseException {
        MathematicalObjectOfInterest moi = node.getNode();
        PrintablePomTaggedExpression pte = moi.getMoi();
        Set<String> replacementPerformed = new HashSet<>();
        LOG.debug("Start replacements on MOI: " + pte.getTexString());

        if ( retrievedMacros.containedEulerMascheroniEvidence() ) {
            LOG.debug("The hit contained an evidence on Euler-Mascheroni constant. Hence we replace all \\gamma by \\EulerConstant");
            GenericConstantReplacer.replaceGammaAsEulerMascheroniConstant(pte);
        }

        GenericReplacementTool genericReplacementTool = new GenericReplacementTool(pte);
        pte = genericReplacementTool.getSemanticallyEnhancedExpression();
        LOG.debug("Replaced general patterns: " + pte.getTexString());

        List<SemanticReplacementRule> macroPatterns = retrievedMacros.getPatterns();
        int counter = 0;
        double score = 0.0;
        for( SemanticReplacementRule semanticReplacementRule : macroPatterns ) {
            MatcherConfig config = MacroHelper.getMatchingConfig(semanticReplacementRule, node);

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
