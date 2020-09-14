package gov.nist.drmf.interpreter.mlp.extensions;

import gov.nist.drmf.interpreter.mlp.FeatureSetUtility;
import mlp.MathTerm;
import mlp.PomTaggedExpression;

import java.util.Iterator;
import java.util.List;

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

        Iterator<PrintablePomTaggedExpression> it = elements.iterator();
        sb.append(it.next().getTexString());
        String prev = sb.toString();

        while ( it.hasNext() ) {
            PrintablePomTaggedExpression p = it.next();
            String s = p.getTexString();
            if ( !s.matches("^[{^_!].*|[)}\\]|@]") && !prev.matches(".*[({\\[|@]$") ) sb.append(" ");
            sb.append(s);
            prev = s;
        }

        return sb.toString().trim();
    }
}
