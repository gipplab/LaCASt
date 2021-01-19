package gov.nist.drmf.interpreter.pom.extensions;

import gov.nist.drmf.interpreter.common.latex.TeXPreProcessor;
import gov.nist.drmf.interpreter.pom.common.FeatureSetUtility;
import gov.nist.drmf.interpreter.pom.common.PomTaggedExpressionUtility;
import gov.nist.drmf.interpreter.pom.common.grammar.ExpressionTags;
import mlp.MathTerm;
import mlp.PomTaggedExpression;

import java.util.*;

/**
 * @author Andre Greiner-Petter
 */
public final class PrintablePomTaggedExpressionUtility {
    private PrintablePomTaggedExpressionUtility() {
    }

    public static String getInternalNodeCommand(MathTerm mt) {
        String cmd = mt.getTermText();
        String val = cmd.isBlank() ? mt.getFeatureValue(FeatureSetUtility.LATEX_FEATURE_KEY) : cmd;
        return val == null ? "" : val;
    }

    public static String getInternalNodeCommand(PomTaggedExpression pte) {
        MathTerm mt = pte.getRoot();
        String val = getInternalNodeCommand(mt);
        if (val.isBlank()) val = pte.getFeatureValue(FeatureSetUtility.LATEX_FEATURE_KEY);
        return val == null ? "" : val;
    }

    public static String buildString(Iterable<PrintablePomTaggedExpression> elements) {
        StringBuilder sb = new StringBuilder();

        Iterator<PrintablePomTaggedExpression> it = elements.iterator();
        sb.append(it.next().getTexString());
        String prev = sb.toString();

        while (it.hasNext()) {
            PrintablePomTaggedExpression p = it.next();

            boolean endedOnEquationSeparator = false;
            if ( p.getParent() != null ) {
                if ( ExpressionTags.equation_array.equalsPTE(p.getParent()) ) {
                    sb.append(" \\\\"); // line break between equation array elements.
                } else if ( addEquationSplitter(p) ) {
                    sb.append(" &");
                    endedOnEquationSeparator = true;
                }
            }

            String s = p.getTexString();
            if (!endedOnEquationSeparator &&
                    !s.matches("^[{^_!].*|[()}\\]|@]") &&
                    !prev.matches(".*[({\\[|@]$")
            ) sb.append(" ");
            sb.append(s);
            prev = s;
        }

        return sb.toString().trim();
    }

    private static boolean addEquationSplitter(PrintablePomTaggedExpression p) {
        return ExpressionTags.equation.equalsPTE(p.getParent())
                && p.getPreviousSibling() != null
                && !p.getTexString().startsWith("&");
    }

    public static boolean isSingleElementInBrackets(PomTaggedExpression pom) {
        String expr = pom.getRoot().getTermText();
        if (pom instanceof PrintablePomTaggedExpression)
            expr = ((PrintablePomTaggedExpression) pom).getTexString();
        return pom.getComponents().isEmpty() && TeXPreProcessor.wrappedInCurlyBrackets(expr);
    }

    public static List<PrintablePomTaggedExpression> deepCopyPPTEList(List<PrintablePomTaggedExpression> list) {
        var copy = new LinkedList<PrintablePomTaggedExpression>();
        list.forEach(pte -> {
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

        if (isIdentifier(pte)) identifiers.add(pte);

        for (PrintablePomTaggedExpression child : pte.getPrintableComponents()) {
            Collection<PrintablePomTaggedExpression> childIds = getIdentifierNodes(child);
            for (PrintablePomTaggedExpression next : childIds)
                if (!identifiers.contains(next)) identifiers.add(next);
        }

        return identifiers;
    }

    /**
     * Builds the string representation for a list of printable PTE. In several cases, the list might by surrounded by
     * the parent node command. You get this node command via {@link #getInternalNodeCommand(PomTaggedExpression)}.
     *
     * So the caption of a {@link PrintablePomTaggedExpression} is essentially using
     * {@link #getInternalNodeCommand(PomTaggedExpression)} for {@param parentNodeCommand} and
     * {@link PrintablePomTaggedExpression#getPrintableComponents()} for {@param ppte}.
     *
     * @param parentNodeCommand the surrounding or leading node command from the parent node. It can be empty but not null.
     *                          If you want to build the caption of a printable PTE, use the
     *                          {@link #getInternalNodeCommand(PomTaggedExpression)} for this PTE.
     * @param ppte the list of nodes that generates the caption.
     *             If you want to build the caption of a printable PTE, use the children of that PTE for this argument
     *             via {@link PrintablePomTaggedExpression#getPrintableComponents()}.
     * @return the caption of the list of nodes and the parent command.
     */
    public static String getCaptionOfPPTEs(String parentNodeCommand, List<PrintablePomTaggedExpression> ppte) {
        String newCaption = parentNodeCommand;
        String elementsCaption = PrintablePomTaggedExpressionUtility.buildString(ppte);

        if ( PomTaggedExpressionUtility.isTeXEnvironmentString(newCaption) ) {
            String[] env = newCaption.split("\\.{3}");
            newCaption = env[0] + elementsCaption + env[1];
        } else if ( !newCaption.isBlank() && !(elementsCaption.trim().startsWith("{") && elementsCaption.trim().endsWith("}")) ) {
            newCaption += "{" + elementsCaption + "}";
        } else newCaption += elementsCaption;
        return newCaption;
    }
}
