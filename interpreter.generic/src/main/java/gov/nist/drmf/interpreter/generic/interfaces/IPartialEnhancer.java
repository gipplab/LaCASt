package gov.nist.drmf.interpreter.generic.interfaces;

import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.interfaces.ITranslator;
import gov.nist.drmf.interpreter.common.pojo.CASResult;
import gov.nist.drmf.interpreter.common.pojo.NumericResult;
import gov.nist.drmf.interpreter.common.pojo.SymbolicCalculation;
import gov.nist.drmf.interpreter.common.exceptions.MinimumRequirementNotFulfilledException;
import gov.nist.drmf.interpreter.generic.mlp.pojo.MLPDependencyGraph;
import gov.nist.drmf.interpreter.generic.mlp.pojo.MOIAnnotation;
import gov.nist.drmf.interpreter.generic.mlp.pojo.MOIPresentations;
import gov.nist.drmf.interpreter.common.pojo.SemanticEnhancedAnnotationStatus;
import gov.nist.drmf.interpreter.pom.moi.MOINode;
import mlp.ParseException;

import java.util.List;

/**
 * @author Andre Greiner-Petter
 */
public interface IPartialEnhancer {
    MOIPresentations generateAnnotatedLatex(String latex, MLPDependencyGraph graph) throws ParseException;

    void appendSemanticLatex(MOIPresentations moi, MOINode<MOIAnnotation> node) throws ParseException;

    default void appendCASRepresentation(MOIPresentations moi, String key, ITranslator translator)
            throws MinimumRequirementNotFulfilledException, TranslationException {
        moi.requires( SemanticEnhancedAnnotationStatus.SEMANTICALLY_ANNOTATED );
        String casReprs = translator.translate(moi.getSemanticLatex());
        CASResult casResult = new CASResult(casReprs);
        moi.addCasRepresentation(key, casResult);
    }

    void appendComputationResults(MOIPresentations moi)
            throws MinimumRequirementNotFulfilledException;

    NumericResult computeNumerically(String semanticLatex, String cas)
            throws MinimumRequirementNotFulfilledException;

    List<SymbolicCalculation> computeSymbolically(String semanticLatex, String cas)
            throws MinimumRequirementNotFulfilledException;

}
