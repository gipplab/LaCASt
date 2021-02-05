package gov.nist.drmf.interpreter.generic.interfaces;

import gov.nist.drmf.interpreter.cas.translation.SemanticLatexTranslator;
import gov.nist.drmf.interpreter.common.TranslationInformation;
import gov.nist.drmf.interpreter.common.eval.NumericResult;
import gov.nist.drmf.interpreter.common.eval.SymbolicResult;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.interfaces.IDLMFTranslator;
import gov.nist.drmf.interpreter.common.interfaces.ITranslator;
import gov.nist.drmf.interpreter.common.pojo.*;
import gov.nist.drmf.interpreter.common.exceptions.MinimumRequirementNotFulfilledException;
import gov.nist.drmf.interpreter.generic.common.GenericFunctionAnnotator;
import gov.nist.drmf.interpreter.generic.mlp.pojo.MLPDependencyGraph;
import gov.nist.drmf.interpreter.generic.mlp.pojo.MOIAnnotation;
import gov.nist.drmf.interpreter.generic.mlp.pojo.MOIPresentations;
import gov.nist.drmf.interpreter.pom.extensions.PrintablePomTaggedExpression;
import gov.nist.drmf.interpreter.pom.moi.MOINode;
import mlp.ParseException;

import javax.print.attribute.HashAttributeSet;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Andre Greiner-Petter
 */
public interface IPartialEnhancer {
    MOIPresentations generateAnnotatedLatex(String latex, MLPDependencyGraph graph) throws ParseException;

    void appendSemanticLatex(MOIPresentations moi, MOINode<MOIAnnotation> node) throws ParseException;

    default void appendCASRepresentation(MOIPresentations moi, String key, IDLMFTranslator<PrintablePomTaggedExpression> translator)
            throws MinimumRequirementNotFulfilledException, TranslationException {
        moi.requires( SemanticEnhancedAnnotationStatus.SEMANTICALLY_ANNOTATED );
        try {
            TranslationInformation casReprsInfo = translator.translateToObjectFeatured(moi.getSemanticLatex(), new GenericFunctionAnnotator());
            MetaTranslationInformation metaInfo = new MetaTranslationInformation(casReprsInfo);
            CASResult casResult = new CASResult(casReprsInfo.getTranslatedExpression());
            casResult.setTranslationInformation(metaInfo);
            moi.addCasRepresentation(key, casResult);
        } catch ( TranslationException te ) {
            MetaTranslationInformation metaInfo = new MetaTranslationInformation();
            Map<String, String> errorInfo = new HashMap<>();
            errorInfo.put("Error", te.toString());
            metaInfo.setTokenTranslationInformation(errorInfo);
            CASResult casResult = new CASResult("");
            casResult.setTranslationInformation(metaInfo);
            moi.addCasRepresentation(key, casResult);
        }
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
