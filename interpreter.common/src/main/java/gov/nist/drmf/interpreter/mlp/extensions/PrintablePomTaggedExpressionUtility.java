package gov.nist.drmf.interpreter.mlp.extensions;

import gov.nist.drmf.interpreter.common.TeXPreProcessor;
import gov.nist.drmf.interpreter.mlp.FeatureSetUtility;
import gov.nist.drmf.interpreter.mlp.PomTaggedExpressionUtility;
import mlp.MathTerm;
import mlp.PomTaggedExpression;

import java.util.*;

/**
 * @author Andre Greiner-Petter
 */
public final class PrintablePomTaggedExpressionUtility {
    private PrintablePomTaggedExpressionUtility(){}

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

    public static boolean isSingleElementInBrackets(PomTaggedExpression pom) {
        String expr = pom.getRoot().getTermText();
        if ( pom instanceof PrintablePomTaggedExpression )
            expr = ((PrintablePomTaggedExpression) pom).getTexString();
        return pom.getComponents().isEmpty() && TeXPreProcessor.wrappedInCurlyBrackets(expr);
    }

    public static List<PrintablePomTaggedExpression> deepCopyPPTEList(List<PrintablePomTaggedExpression> list) {
        var copy = new LinkedList<PrintablePomTaggedExpression>();
        list.forEach( pte -> {
            PrintablePomTaggedExpression c = new PrintablePomTaggedExpression(pte);
            copy.add(c);
        });
        return copy;
    }

    public static boolean isIdentifier(PrintablePomTaggedExpression pte) {
        return PomTaggedExpressionUtility.isSingleVariable(pte);
    }

    public static Collection<PrintablePomTaggedExpression> getIdentifierNodes(PrintablePomTaggedExpression pte) {
        Collection<PrintablePomTaggedExpression> identifiers = new LinkedList<>();

        if ( isIdentifier(pte) ) identifiers.add(pte);

        for ( PrintablePomTaggedExpression child : pte.getPrintableComponents() ) {
            Collection<PrintablePomTaggedExpression> childIds = getIdentifierNodes(child);
            for ( PrintablePomTaggedExpression next : childIds )
                if ( !identifiers.contains(next) ) identifiers.add(next);
        }

        return identifiers;
    }
}
