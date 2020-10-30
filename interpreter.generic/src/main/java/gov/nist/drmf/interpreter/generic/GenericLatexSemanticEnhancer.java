package gov.nist.drmf.interpreter.generic;

import gov.nist.drmf.interpreter.pom.extensions.PrintablePomTaggedExpression;
import mlp.ParseException;

/**
 * @author Andre Greiner-Petter
 */
public class GenericLatexSemanticEnhancer implements IGenericLatexSemanticEnhancerAPI {
    @Override
    public PrintablePomTaggedExpression enhanceGenericLaTeX(String latex, String context, String dlmfLabel) throws ParseException {
        throw new RuntimeException("enhanceGenericLaTeX is not yet implemented.");
    }
}
