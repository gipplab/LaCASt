package gov.nist.drmf.interpreter.mlp.extensions;

import mlp.MathTerm;
import mlp.PomTaggedExpression;

/**
 * @author Andre Greiner-Petter
 */
public final class PrintablePomTaggedExpressionUtils {
    private PrintablePomTaggedExpressionUtils(){}

    public static String getInternalNodeCommand(MathTerm mt) {
        String cmd = mt.getTermText();
        String val = cmd.isBlank() ? mt.getFeatureValue(FeatureSetUtility.LATEX_FEATURE_KEY) : cmd;
        return val == null ? "" : val;
    }

    public static String getInternalNodeCommand(PomTaggedExpression pte) {
        MathTerm mt = pte.getRoot();
        String val = getInternalNodeCommand(mt);
        if ( val.isBlank() ) val = pte.getFeatureValue(FeatureSetUtility.LATEX_FEATURE_KEY);
        return val == null ? "" : val;
    }

    public static String buildString(Iterable<PrintablePomTaggedExpression> elements) {
        StringBuilder sb = new StringBuilder();
        for (PrintablePomTaggedExpression ppte : elements) {
            sb.append(ppte.getTexString());
        }
        return sb.toString();
    }
}
