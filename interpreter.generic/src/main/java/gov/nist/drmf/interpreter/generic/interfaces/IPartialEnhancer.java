package gov.nist.drmf.interpreter.generic.interfaces;

import gov.nist.drmf.interpreter.common.cas.ICASEngineSymbolicEvaluator;
import gov.nist.drmf.interpreter.common.eval.INumericTestCalculator;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.interfaces.ITranslator;
import gov.nist.drmf.interpreter.common.pojo.CASResult;
import gov.nist.drmf.interpreter.generic.exceptions.MinimumRequirementNotFulfilledException;
import gov.nist.drmf.interpreter.generic.mlp.pojo.MLPDependencyGraph;
import gov.nist.drmf.interpreter.generic.mlp.pojo.MOIAnnotation;
import gov.nist.drmf.interpreter.generic.mlp.pojo.MOIPresentations;
import gov.nist.drmf.interpreter.generic.mlp.pojo.SemanticEnhancedAnnotationStatus;
import gov.nist.drmf.interpreter.pom.moi.MOINode;
import mlp.ParseException;

/**
 * @author Andre Greiner-Petter
 */
public interface IPartialEnhancer {
    MOIPresentations generateAnnotatedLatex(String latex, MLPDependencyGraph graph) throws ParseException;

    void appendSemanticLatex(MOIPresentations moi, MOINode<MOIAnnotation> node) throws ParseException;

    default void appendCASRepresentation(MOIPresentations moi, String key, ITranslator translator)
            throws MinimumRequirementNotFulfilledException, TranslationException {
        checkAgainstSemanticStatus(moi, SemanticEnhancedAnnotationStatus.SEMANTICALLY_ANNOTATED);
        String casReprs = translator.translate(moi.getSemanticLatex());
        CASResult casResult = new CASResult(casReprs);
        moi.addCasRepresentation(key, casResult);
    }

    CASResult computeNumerically(String semanticLatex, CASResult casResult, INumericTestCalculator<?> numericTestCalculator);

    CASResult computeSemantically(String semanticLatex, CASResult casResult, ICASEngineSymbolicEvaluator<?> symbolicEvaluator);

    default void checkAgainstSemanticStatus(MOIPresentations moi, SemanticEnhancedAnnotationStatus min) {
        if ( !moi.getStatus().hasPassed( min ) ) throw new MinimumRequirementNotFulfilledException(min, moi.getStatus());
    }
}
