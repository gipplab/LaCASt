package gov.nist.drmf.interpreter.mlp.extensions;

import gov.nist.drmf.interpreter.common.interfaces.IMatcher;
import gov.nist.drmf.interpreter.mlp.FeatureSetUtility;
import mlp.MathTerm;
import mlp.PomTaggedExpression;

import java.util.Arrays;
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
public class PrintablePomTaggedExpression extends PomTaggedExpression implements IMatcher<PrintablePomTaggedExpression> {
    private final LinkedList<PrintablePomTaggedExpression> printableComponents;

    private String caption;

    /**
     * Copy constructor
     * @param ppte a previously valid printable PoM expression
     */
    public PrintablePomTaggedExpression( PrintablePomTaggedExpression ppte ) {
        super(new MathTerm(ppte.getRoot().getTermText(), ppte.getRoot().getTag()), ppte.getTag(), ppte.getSecondaryTags());
        this.caption = ppte.caption;
        this.printableComponents = new LinkedList<>();
        for ( PrintablePomTaggedExpression child : ppte.getPrintableComponents() ) {
            PrintablePomTaggedExpression childCopy = new PrintablePomTaggedExpression(child);
            this.printableComponents.add(childCopy);
            super.addComponent(childCopy);
        }
    }

    public PrintablePomTaggedExpression( MathTerm mathTerm, String... exprTags ) {
        super(mathTerm, exprTags);
        this.printableComponents = new LinkedList<>();
        this.caption = mathTerm.getTermText();
    }

    public PrintablePomTaggedExpression( PomTaggedExpression pte, String expr ) {
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

            Pattern thisPattern = Pattern.compile(generatePattern(thisMatch));
            Pattern nextPattern = Pattern.compile(nextMatch);

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

            if (isStartingIndexOpenBracket(idxStart, expr) && isEndingIndexCloseBracket(idxEnd, expr)){
                idxStart--;
                idxEnd++;
            }

            idxEnd = checkIndexForClosingBrackets(idxStart, idxEnd, expr);

            innerExpression = expr.substring(idxStart, idxEnd);
            expr = expr.substring(idxEnd);

            PrintablePomTaggedExpression ppte = new PrintablePomTaggedExpression(component, innerExpression);
            super.addComponent(ppte);
            printableComponents.add(ppte);
        }
    }

    private boolean isStartingIndexOpenBracket(int idxStart, String expr) {
        return idxStart > 0 && (expr.charAt(idxStart-1) == '[' || expr.charAt(idxStart-1) == '{' );
    }

    private boolean isEndingIndexCloseBracket(int idxEnd, String expr) {
        return idxEnd < expr.length() && (expr.charAt(idxEnd) == ']' || expr.charAt(idxEnd) == '}');
    }

    private static String generatePattern(String input) {
        if ( input.matches("[A-Za-z]+") ) {
            return "(?<![A-Za-z])"+input+"(?![A-Za-z])";
        } else return "\\Q"+input+"\\E";
    }

    private String getStartingString(PomTaggedExpression pte) {
        MathTerm mt = pte.getRoot();
        String token = mt.getTermText();
        if (token.isBlank()) {
            // might contain a latex feature, let's check it
            String latexFeature = pte.getFeatureValue(FeatureSetUtility.LATEX_FEATURE_KEY);
            if (latexFeature != null) token = latexFeature;
        } else if (mt.wasFontActionApplied()) {
            // is there a font-action applied?
            token = mt.firstFontAction() + "{" + token + "}";
        }

        return checkSubExpressionToken(token, pte);
    }

    private String checkSubExpressionToken(String token, PomTaggedExpression pte) {
        if (token.isBlank()) {
            if (pte.getComponents().isEmpty()) {
                // well, a blank token with no components is only possible by "{}". So we shall
                // return this, I guess.
                return "{";
            } else return getStartingString(pte.getComponents().get(0));
        } else return token;
    }

    private String getEndingString(PomTaggedExpression pte) {
        List<PomTaggedExpression> components = pte.getComponents();
        if (components.isEmpty()) {
            String p = generatePattern(getStartingString(pte));
            // this only happens for empty expression. Hence, we want the } symbol here.
            if ( p.equals("\\Q{\\E") ) return "\\Q}\\E";
            return p;
        } else {
            int fromBehind = 1;
            PomTaggedExpression lastElement = components.get(components.size() - fromBehind);

            // if the last element has children, go a step deeper
            if ( !lastElement.getComponents().isEmpty() ) {
                return getEndingString(lastElement);
            }

            LinkedList<PomTaggedExpression> latestElements = new LinkedList<>();
            latestElements.addFirst(lastElement);
            fromBehind++;
            // if the last element is a leaf, we can take as many previous leaves as we want
            while ( lastElement.getComponents().isEmpty() && fromBehind <= components.size() ) {
                lastElement = components.get(components.size() - fromBehind);
                latestElements.addFirst(lastElement); // opposite order
                fromBehind++;
            }

            StringBuilder entireListOfComponents = new StringBuilder();
            for ( int i = 0; i < latestElements.size()-1; i++ ) {
                entireListOfComponents.append(getEndingString(latestElements.get(i)));
                entireListOfComponents.append("[\\s{}\\[\\]]*");
            }
            entireListOfComponents.append(getEndingString(latestElements.getLast()));

            return entireListOfComponents.toString();
        }
    }

    private int checkIndexForClosingBrackets(int start, int end, String expression) {
        if (expression.length() == 0) return 0;

        String sub = expression.substring(start, end);
        int opened = countOpenBrackets(sub);

        return getEndIndex(opened, end, expression);
    }

    private int countOpenBrackets(String sub) {
        int opened = 0;
        for (int i = 0; i < sub.length(); i++) {
            if (sub.charAt(i) == '{') opened++;
            else if (sub.charAt(i) == '}') opened--;
        }
        return opened;
    }

    private int getEndIndex(int opened, int end, String expression) {
        while (opened > 0 && end < expression.length()) {
            if (expression.charAt(end) == '}') {
                end++;
                opened--;
            } else end++;
        }

        return end;
    }

    @Override
    public boolean match(PrintablePomTaggedExpression expression) {
        MatchablePomTaggedExpression m = new MatchablePomTaggedExpression(this, "");
        return m.match(expression);
    }

    @Override
    public void setComponents(List<PomTaggedExpression> components) {
        innerSetComponents(components);
        super.setComponents(components);
        populatingStringChanges();
    }

    @Override
    public void setComponents(PomTaggedExpression... components) {
        innerSetComponents(Arrays.asList(components));
        super.setComponents(components);
        populatingStringChanges();
    }

    private void innerSetComponents(Iterable<PomTaggedExpression> components) {
        printableComponents.clear();
        for (PomTaggedExpression pte : components) {
            if (!(pte instanceof PrintablePomTaggedExpression))
                throw new IllegalArgumentException("Printable tree must contain only printable elements.");
            else {
                printableComponents.add((PrintablePomTaggedExpression)pte);
            }
        }
    }

    @Override
    public boolean addComponent(PomTaggedExpression pte) {
        if (pte instanceof PrintablePomTaggedExpression) {
            boolean res = super.addComponent(pte);
            if (res) {
                printableComponents.add((PrintablePomTaggedExpression) pte);
                populatingStringChanges();
            }
            return res;
        } else {
            throw new IllegalArgumentException("Printable tree must contain only printable elements.");
        }
    }

    @Override
    public boolean addComponent(int i, PomTaggedExpression pte) {
        if (pte instanceof PrintablePomTaggedExpression) {
            boolean res = super.addComponent(i, pte);
            if (res) {
                printableComponents.add(i, (PrintablePomTaggedExpression) pte);
                populatingStringChanges();
            }
            return res;
        } else {
            throw new IllegalArgumentException("Printable tree must contain only printable elements.");
        }
    }

    @Override
    public void set(PomTaggedExpression pte) {
        if (pte instanceof PrintablePomTaggedExpression) {
            super.set(pte);
            printableComponents.clear();
            for ( PomTaggedExpression comp : pte.getComponents() ) {
                printableComponents.add((PrintablePomTaggedExpression)comp);
            }
            populatingStringChanges();
        } else {
            throw new IllegalArgumentException("Printable tree must contain only printable elements.");
        }
    }

    @Override
    public void setRoot(MathTerm mathTerm) {
        String newCaption = PrintablePomTaggedExpressionUtils.getInternalNodeCommand(mathTerm);
        replaceCaption(newCaption);
        if ( getParent() != null ) {
            PrintablePomTaggedExpression parent = (PrintablePomTaggedExpression)getParent();
            parent.populatingStringChanges();
        }

        for ( PrintablePomTaggedExpression ppte : printableComponents ){
            this.caption += ppte.caption;
        }

        super.setRoot(mathTerm);
    }

    private void replaceCaption(String newCaption) {
        if ( caption.matches("[\\[{].*[]}]") ){
            String start = caption.substring(0,1);
            String end = caption.substring(caption.length()-1);
            this.caption = start + newCaption + end;
        } else this.caption = newCaption;
    }

    /**
     * Populates string changes from here onwards to the root of the tree.
     */
    private void populatingStringChanges() {
        if ( !printableComponents.isEmpty() ) {
            String newCaption = PrintablePomTaggedExpressionUtils.getInternalNodeCommand(this);
            newCaption += PrintablePomTaggedExpressionUtils.buildString(printableComponents);
            replaceCaption(newCaption);
        }

        if ( this.getParent() != null ) {
            PrintablePomTaggedExpression parent = (PrintablePomTaggedExpression)this.getParent();
            parent.populatingStringChanges();
        }
    }

    public LinkedList<PrintablePomTaggedExpression> getPrintableComponents() {
        return this.printableComponents;
    }

    public String getTexString() {
        return caption;
    }
}
