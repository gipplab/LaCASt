package gov.nist.drmf.interpreter.generic;

import gov.nist.drmf.interpreter.pom.extensions.PrintablePomTaggedExpression;
import mlp.ParseException;

/**
 * @author Andre Greiner-Petter
 */
public interface IGenericLatexSemanticEnhancerAPI {
    /**
     * Enhances the given generic LaTeX string by the given context and returns a semantic LaTeX parsed
     * expression.
     * @param latex the generic latex string
     * @param context the textual context
     * @return semantically enhanced parsed LaTeX expression
     * @throws ParseException if the expressions cannot be parsed
     */
    default PrintablePomTaggedExpression enhanceGenericLaTeX(String latex, String context) throws ParseException {
        return enhanceGenericLaTeX(latex, context, null);
    }

    /**
     * Enhances the given generic LaTeX string by the given context and returns a semantic LaTeX parsed
     * expression. By providing a label of a DLMF equation, it triggers auto-replacement rules in the context
     * of the DLMF, such as <code>i</code> become <code>\iunit</code>.
     *
     * @param latex the generic latex string
     * @param context the textual context
     * @param dlmfLabel a label of a DLMF equation (can be null)
     * @return semantically enhanced parsed LaTeX expression
     * @throws ParseException if the expressions cannot be parsed
     */
    PrintablePomTaggedExpression enhanceGenericLaTeX(String latex, String context, String dlmfLabel) throws ParseException;

    /**
     * Enhances the given generic LaTeX string by the given context and returns a semantic LaTeX string.
     * @param latex the generic latex string
     * @param context the textual context
     * @return semantically enhanced LaTeX string
     * @throws ParseException if the expressions cannot be parsed
     */
    default String enhanceGenericLaTeXToString(String latex, String context) throws ParseException {
        return enhanceGenericLaTeXToString(latex, context, null);
    }

    /**
     * Enhances the given generic LaTeX string by the given context and returns a semantic LaTeX
     * expression. By providing a label of a DLMF equation, it triggers auto-replacement rules in the context
     * of the DLMF, such as <code>i</code> become <code>\iunit</code>.
     * @param latex the generic latex string
     * @param context the textual context
     * @param dlmfLabel a label of a DLMF equation (can be null)
     * @return semantically enhanced LaTeX string
     * @throws ParseException if the expressions cannot be parsed
     */
    default String enhanceGenericLaTeXToString(String latex, String context, String dlmfLabel) throws ParseException {
        return enhanceGenericLaTeX(latex, context, dlmfLabel).getTexString();
    }
}
