package gov.nist.drmf.interpreter.mlp.extensions;

import gov.nist.drmf.interpreter.common.grammar.ExpressionTags;
import mlp.MathTerm;
import mlp.PomTaggedExpression;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * To create an instance of this class, it is advisable to use the wrapper classes
 * {@link gov.nist.drmf.interpreter.mlp.MLPWrapper} or
 * {@link gov.nist.drmf.interpreter.mlp.SemanticMLPWrapper}.
 *
 * @author Andre Greiner-Petter
 * @see gov.nist.drmf.interpreter.mlp.MLPWrapper
 * @see gov.nist.drmf.interpreter.mlp.SemanticMLPWrapper
 * @see PomTaggedExpression
 */
public class PrintablePomTaggedExpression extends PomTaggedExpression {
    public static final String LATEX_FEATURE = "LaTeX";

    private List<PrintablePomTaggedExpression> printableComponents;

    private String caption;

    public PrintablePomTaggedExpression(PomTaggedExpression pte, String expr) {
        super();
        super.setRoot(pte.getRoot());
        super.setTag(pte.getTag());
        super.setSecondaryTags(pte.getSecondaryTags());
        for (String k : pte.getNamedFeatures().keySet())
            super.addNamedFeature(k, pte.getFeatureValue(k));

        // the fun part, every node has it's own caption
        expr = expr.trim();
        this.caption = expr;
        this.printableComponents = new LinkedList<>();

        // now we have to add the components and their respective substrings...
        for (PomTaggedExpression component : pte.getComponents()) {
            String innerExpression;

            String thisMatch = getStartingString(component);
            String nextMatch = getEndingString(component);

            Pattern thisPattern = Pattern.compile(thisMatch, Pattern.LITERAL);
            Pattern nextPattern = Pattern.compile(nextMatch, Pattern.LITERAL);

            Matcher thisM = thisPattern.matcher(expr);
            Matcher nextM = nextPattern.matcher(expr);

            int idxStart = 0;
            int idxEnd = expr.length();

            if (thisM.find()) {
                idxStart = thisM.start();
            }

            if (nextM.find()) {
                idxEnd = nextM.end();
            }

            if (idxStart == idxEnd) {
                // essentially means, getEnding and getStart provide the same string
                idxEnd += thisMatch.length();
            }

            idxEnd = checkIndexForClosingBrackets(idxStart, idxEnd, expr);

            innerExpression = expr.substring(idxStart, idxEnd);
            expr = expr.substring(idxEnd);

            PrintablePomTaggedExpression ppte =
                    new PrintablePomTaggedExpression(component, innerExpression);
            addComponent(ppte);
        }
    }

    private String getStartingString(PomTaggedExpression pte) {
        MathTerm mt = pte.getRoot();
        String token = mt.getTermText();
        if (token.isBlank()) {
            // might contain a latex feature, let's check it
            String latexFeature = pte.getFeatureValue(LATEX_FEATURE);
            if (latexFeature != null) token = latexFeature;
        } else if (mt.wasFontActionApplied()) {
            // is there a font-action applied?
            token = mt.firstFontAction() + "{" + token + "}";
        }

        if (token.isBlank()) {
            if (pte.getComponents().isEmpty())
                throw new IllegalArgumentException("Cannot find starting string of this expression " + pte);
            return getStartingString(pte.getComponents().get(0));
        } else return token;
    }

    private String getEndingString(PomTaggedExpression pte) {
        List<PomTaggedExpression> components = pte.getComponents();
        if (components.isEmpty()) {
            MathTerm mt = pte.getRoot();
            return mt.getTermText();
        } else {
            return getEndingString(components.get(components.size() - 1));
        }
    }

    private int checkIndexForClosingBrackets(int start, int end, String expression) {
        if (expression.length() == 0) return 0;
        String sub = expression.substring(start, end);
        int opened = 0;
        for (int i = 0; i < sub.length(); i++) {
            if (sub.charAt(i) == '{') opened++;
            if (sub.charAt(i) == '}') opened--;
        }

        while (opened > 0 && end < expression.length()) {
            if (expression.charAt(end) == '}') {
                end++;
                opened--;
            } else end++;
        }

        return end;
    }

    @Override
    public void setComponents(List<PomTaggedExpression> components) {
        for (PomTaggedExpression pte : components) {
            if (!(pte instanceof PrintablePomTaggedExpression))
                throw new IllegalArgumentException("Printable tree must contain only printable elements.");
        }
        super.setComponents(components);
    }

    @Override
    public void setComponents(PomTaggedExpression... components) {
        for (PomTaggedExpression pte : components) {
            if (!(pte instanceof PrintablePomTaggedExpression))
                throw new IllegalArgumentException("Printable tree must contain only printable elements.");
        }
        super.setComponents(components);
    }

    @Override
    public boolean addComponent(PomTaggedExpression pte) {
        if (pte instanceof PrintablePomTaggedExpression) {
            boolean res = super.addComponent(pte);
            if (res) printableComponents.add((PrintablePomTaggedExpression) pte);
            return res;
        } else {
            throw new IllegalArgumentException("Printable tree must contain only printable elements.");
        }
    }

    @Override
    public boolean addComponent(int i, PomTaggedExpression pte) {
        if (pte instanceof PrintablePomTaggedExpression) {
            boolean res = super.addComponent(i, pte);
            if (res) printableComponents.add(i, (PrintablePomTaggedExpression) pte);
            return res;
        } else {
            throw new IllegalArgumentException("Printable tree must contain only printable elements.");
        }
    }

    @Override
    public void set(PomTaggedExpression pte) {
        if (pte instanceof PrintablePomTaggedExpression) {
            super.set(pte);
        } else {
            throw new IllegalArgumentException("Printable tree must contain only printable elements.");
        }
    }

    public List<PrintablePomTaggedExpression> getPrintableComponents() {
        return this.printableComponents;
    }

    public String getTexString() {
        return caption;
    }
}
