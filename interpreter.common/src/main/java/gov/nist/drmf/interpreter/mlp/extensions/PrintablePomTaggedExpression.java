package gov.nist.drmf.interpreter.mlp.extensions;

import gov.nist.drmf.interpreter.common.TeXPreProcessor;
import gov.nist.drmf.interpreter.common.interfaces.IMatcher;
import gov.nist.drmf.interpreter.mlp.FeatureSetUtility;
import gov.nist.drmf.interpreter.mlp.PomTaggedExpressionUtility;
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
    private String caption;

    /**
     * Copy constructor
     * @param ppte a previously valid printable PoM expression
     */
    public PrintablePomTaggedExpression( PrintablePomTaggedExpression ppte ) {
        super(new MathTerm(ppte.getRoot().getTermText(), ppte.getRoot().getTag()), ppte.getTag(), ppte.getSecondaryTags());
        this.caption = ppte.caption;
        ppte.getNamedFeatures().entrySet().forEach( e -> super.addNamedFeature(e.getKey(), e.getValue()) );
        for ( PrintablePomTaggedExpression child : ppte.getPrintableComponents() ) {
            PrintablePomTaggedExpression childCopy = new PrintablePomTaggedExpression(child);
            super.addComponent(childCopy);
        }
    }

    public PrintablePomTaggedExpression( MathTerm mathTerm, String... exprTags ) {
        super(mathTerm, exprTags);
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
        if ( pte.getParent() == null )
            expr = TeXPreProcessor.trimCurlyBrackets(expr);
        else expr = expr.trim();
        this.caption = expr;

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
        } else return Pattern.quote(input);
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
            if ( p.equals("\\Q{\\E") ) return Pattern.quote("}");
            return p;
        } else {
            StringBuilder entireListOfComponents = new StringBuilder();
            String potentialRoot = PrintablePomTaggedExpressionUtils.getInternalNodeCommand(pte);
            entireListOfComponents.append(Pattern.quote(potentialRoot));
            if ( !potentialRoot.isBlank() ) entireListOfComponents.append("[\\s{}\\[\\]]*");

            for ( int i = 0; i < components.size(); i++ ) {
                PomTaggedExpression last = components.get(i);
                if ( PomTaggedExpressionUtility.isSequence(last) ) {
                    for ( PomTaggedExpression child : last.getComponents() ) {
                        entireListOfComponents.append("[\\s{}\\[\\]]*")
                                .append(getEndingString(child));
                    }
                } else if ( !last.getComponents().isEmpty() ) {
                    String root = PrintablePomTaggedExpressionUtils.getInternalNodeCommand(last);
                    entireListOfComponents.append(Pattern.quote(root));
                    for ( PomTaggedExpression child : last.getComponents() ) {
                        entireListOfComponents.append("[\\s{}\\[\\]]*")
                                .append(getEndingString(child));
                    }
                } else {
                    entireListOfComponents.append(getEndingString(components.get(i)));
                }
                if ( i < components.size()-1 )
                    entireListOfComponents.append("[\\s{}\\[\\]]*");
            }

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

    public void clearComponents(){
        super.getComponents().clear();
        this.caption = "";
    }

    /*
     * ==================================================================
     * Take care that it is literally impossible to add non printable objects
     * to this node. Hence we need to overwrite all modifying methods.
     */

    /**
     * @return true if this node has no children, otherwise false.
     */
    public boolean hasNoChildren() {
        return super.getComponents().isEmpty();
    }

    @Override
    public boolean match(PrintablePomTaggedExpression expression) {
        MatchablePomTaggedExpression m = new MatchablePomTaggedExpression(this, "");
        return m.match(expression);
    }

    /**
     * Provides an adapter for the {@link #setComponents(List)} method, which only
     * allows components of type {@link PomTaggedExpression} rather than
     * {@code List<? extends PomTaggedExpression>}.
     * @param components a list of components.
     */
    @SuppressWarnings("unchecked")
    public void setPrintableComponents(List<? extends PomTaggedExpression> components) {
        this.setComponents( (List<PomTaggedExpression>) components );
    }

    /**
     * Provides an adapter for the {@link #setComponents(PomTaggedExpression...)} )} method, which only
     * allows components of type {@link PomTaggedExpression} rather. It throws an error if any
     * element is not of type {@link PrintablePomTaggedExpression}.
     * @param components a list of components.
     */
    public void setPrintableComponents(PomTaggedExpression... components) {
        this.setPrintableComponents(Arrays.asList(components));
    }

    /**
     * @deprecated use {@link #setPrintableComponents(List)} instead.
     */
    @Override
    @Deprecated
    public void setComponents(List<PomTaggedExpression> components) {
        checkComponentValidity(components);
        super.setComponents(components);
        populatingStringChanges();
    }

    /**
     * @deprecated use {@link #setPrintableComponents(List)} instead.
     */
    @Override
    @Deprecated
    public void setComponents(PomTaggedExpression... components) {
        checkComponentValidity(Arrays.asList(components));
        super.setComponents(components);
        populatingStringChanges();
    }

    private void checkComponentValidity(Iterable<PomTaggedExpression> components) throws IllegalArgumentException {
        for (PomTaggedExpression pte : components) {
            checkComponentValidity(pte);
        }
    }

    private void checkComponentValidity(PomTaggedExpression component) throws IllegalArgumentException {
        if (!(component instanceof PrintablePomTaggedExpression))
            throw new IllegalArgumentException("Printable tree must contain only printable elements.");
    }

    @Override
    public boolean addComponent(PomTaggedExpression pte) {
        checkComponentValidity(pte);
        boolean res = super.addComponent(pte);
        if (res) {
            populatingStringChanges();
        }
        return res;
    }

    @Override
    public boolean addComponent(int i, PomTaggedExpression pte) {
        checkComponentValidity(pte);
        boolean res = super.addComponent(i, pte);
        if (res) {
            populatingStringChanges();
        }
        return res;
    }

    @Override
    public void addComponent(int i, MathTerm term) {
        PrintablePomTaggedExpression pte = new PrintablePomTaggedExpression(term);
        this.addComponent(i, pte);
    }

    @Override
    public void set(PomTaggedExpression pte) {
        checkComponentValidity(pte);
        super.set(pte);
        populatingStringChanges();
    }

    @Override
    public void setRoot(MathTerm mathTerm) {
        String newCaption = PrintablePomTaggedExpressionUtils.getInternalNodeCommand(mathTerm);
        replaceCaption(newCaption);
        if ( getParent() != null ) {
            PrintablePomTaggedExpression parent = (PrintablePomTaggedExpression) getParent();
            parent.populatingStringChanges();
        }

        for ( PrintablePomTaggedExpression ppte : getPrintableComponents() ){
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
        if ( !hasNoChildren() ) {
            String newCaption = PrintablePomTaggedExpressionUtils.getInternalNodeCommand(this);
            newCaption += PrintablePomTaggedExpressionUtils.buildString(getPrintableComponents());
            replaceCaption(newCaption);
        }

        if ( this.getParent() != null ) {
            PrintablePomTaggedExpression parent = (PrintablePomTaggedExpression)this.getParent();
            parent.populatingStringChanges();
        }
    }

    /**
     * This is rather tricky. Unfortunately, the internal {@link PomTaggedExpression#getComponents()}
     * do not allow elements of subclasses such as this printable class. Unless the super class declares
     * the internal components via {@code List<? extends PomTaggedExpression>}, we cannot simply cast
     * the instance like:
     * <pre>{@code
     *     List<PrintablePomTaggedExpression> casted = (List<PrintablePomTaggedExpression>) super.getComponents();
     * }</pre>
     *
     * On the other hand, carrying a copy of the components makes it very hard to keep them synced.
     * Hence, we use a hacky workaround here and cast to a generic {@code List<?>} first.
     *
     * Note that this list is NOT a copy of the original components get from {@link PomTaggedExpression#getComponents()}.
     * It is the same reference.
     * @return returns the list of components, cast to a list of {@link PrintablePomTaggedExpression}.
     */
    @SuppressWarnings("unchecked")
    public List<PrintablePomTaggedExpression> getPrintableComponents() {
        return (List<PrintablePomTaggedExpression>)(List<?>) super.getComponents();
    }

    void makeBalancedTexString() {
        if ( caption.matches("^\\s*\\{.*}\\s*$") ) return;
        caption = "{" + caption + "}";
        populatingStringChanges();
    }

    /**
     * Should be not misunderstood with {@link #getTextTokens()}! This provides access to the actual
     * LaTeX string that generated this parse tree. The whole idea of implementing a printable version
     * of {@link PomTaggedExpression} was just this method.
     * @return the TeX string from this node. If this node is not a leaf, it contains the compound strings
     * from it's children.
     */
    public String getTexString() {
        return caption;
    }
}
