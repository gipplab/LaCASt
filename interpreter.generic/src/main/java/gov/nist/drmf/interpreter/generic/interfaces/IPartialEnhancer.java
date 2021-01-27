package gov.nist.drmf.interpreter.generic.interfaces;

import gov.nist.drmf.interpreter.common.TranslationInformation;
import gov.nist.drmf.interpreter.common.eval.NumericResult;
import gov.nist.drmf.interpreter.common.eval.SymbolicResult;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.interfaces.ITranslator;
import gov.nist.drmf.interpreter.common.pojo.*;
import gov.nist.drmf.interpreter.common.exceptions.MinimumRequirementNotFulfilledException;
import gov.nist.drmf.interpreter.generic.mlp.pojo.MLPDependencyGraph;
import gov.nist.drmf.interpreter.generic.mlp.pojo.MOIAnnotation;
import gov.nist.drmf.interpreter.generic.mlp.pojo.MOIPresentations;
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
        moi.requires( SemanticEnhancedAnnotationStatus.SEMANTICALLY_ANNOTATED );
        TranslationInformation casReprsInfo = translator.translateToObject(moi.getSemanticLatex());
        MetaTranslationInformation metaInfo = new MetaTranslationInformation(casReprsInfo);
        CASResult casResult = new CASResult(casReprsInfo.getTranslatedExpression());
        casResult.setTranslationInformation(metaInfo);
        moi.addCasRepresentation(key, casResult);
    }

    void appendComputationResults(MOIPresentations moi)
            throws MinimumRequirementNotFulfilledException;

    void appendComputationResults(MOIPresentations moi, String cas)
            throws MinimumRequirementNotFulfilledException;

    NumericResult computeNumerically(String semanticLatex, String cas)
            throws MinimumRequirementNotFulfilledException;

    SymbolicResult computeSymbolically(String semanticLatex, String cas)
            throws MinimumRequirementNotFulfilledException;

}
