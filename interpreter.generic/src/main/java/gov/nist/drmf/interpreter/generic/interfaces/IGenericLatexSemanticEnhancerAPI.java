package gov.nist.drmf.interpreter.generic.interfaces;

import gov.nist.drmf.interpreter.generic.mlp.struct.MOIPresentations;
import mlp.ParseException;

/**
 * @author Andre Greiner-Petter
 */
public interface IGenericLatexSemanticEnhancerAPI {
    /**
     * Enhances the given generic LaTeX string by the given context and returns a semantic LaTeX parsed
     * expression.
     * @param context the textual context
     * @param latex the generic latex string
     * @return semantically enhanced parsed LaTeX expression
     * @throws ParseException if the expressions cannot be parsed
     */
    default MOIPresentations enhanceGenericLaTeX(String context, String latex) throws ParseException {
        return enhanceGenericLaTeX(context, latex, null);
    }

    /**
     * Enhances the given generic LaTeX string by the given context and returns a semantic LaTeX parsed
     * expression. By providing a label of a DLMF equation, it triggers auto-replacement rules in the context
     * of the DLMF, such as <code>i</code> become <code>\iunit</code>.
     *
     * @param context the textual context
     * @param latex the generic latex string
     * @param dlmfLabel a label of a DLMF equation (can be null)
     * @return semantically enhanced parsed LaTeX expression
     * @throws ParseException if the expressions cannot be parsed
     */
    MOIPresentations enhanceGenericLaTeX(String context, String latex, String dlmfLabel) throws ParseException;

    /**
     * Enhances the given generic LaTeX string by the given context and returns a semantic LaTeX string.
     * @param context the textual context
     * @param latex the generic latex string
     * @return semantically enhanced LaTeX string
     * @throws ParseException if the expressions cannot be parsed
     */
    default String enhanceGenericLaTeXToString(String context, String latex) throws ParseException {
        return enhanceGenericLaTeXToString(context, latex, null);
    }

    /**
     * Enhances the given generic LaTeX string by the given context and returns a semantic LaTeX
     * expression. By providing a label of a DLMF equation, it triggers auto-replacement rules in the context
     * of the DLMF, such as <code>i</code> become <code>\iunit</code>.
     * @param context the textual context
     * @param latex the generic latex string
     * @param dlmfLabel a label of a DLMF equation (can be null)
     * @return semantically enhanced LaTeX string
     * @throws ParseException if the expressions cannot be parsed
     */
    default String enhanceGenericLaTeXToString(String context, String latex, String dlmfLabel) throws ParseException {
        return enhanceGenericLaTeX(context, latex, dlmfLabel).getSemanticLatex();
    }
}
