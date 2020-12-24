package gov.nist.drmf.interpreter.pom.extensions;

import gov.nist.drmf.interpreter.common.latex.TeXPreProcessor;
import gov.nist.drmf.interpreter.common.interfaces.IMatcher;
import gov.nist.drmf.interpreter.common.text.IndexRange;
import gov.nist.drmf.interpreter.pom.common.MathTermUtility;
import gov.nist.drmf.interpreter.pom.common.PomTaggedExpressionUtility;
import mlp.MathTerm;
import mlp.PomTaggedExpression;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Generates a printable version of {@link PomTaggedExpression}, which means that every node in this
 * parse tree has a string representation that matches the original LaTeX string that was used to
 * generate this parse tree.
 *
 * To create an instance of this class, it is advisable to use the wrapper classes
 * {@link gov.nist.drmf.interpreter.pom.MLPWrapper} or
 * {@link gov.nist.drmf.interpreter.pom.SemanticMLPWrapper}.
 *
 * @author Andre Greiner-Petter
 * @see PomTaggedExpression
 * @see gov.nist.drmf.interpreter.pom.MLPWrapper
 * @see gov.nist.drmf.interpreter.pom.SemanticMLPWrapper
 */
public class PrintablePomTaggedExpression extends PomTaggedExpression implements IMatcher<PrintablePomTaggedExpression> {
    private static final PrintablePomTaggedExpressionRangeCalculator rangeCalculator =
            new PrintablePomTaggedExpressionRangeCalculator();

    private String caption;

    /**
     * Keep Kryo happy for serialization
     */
    private PrintablePomTaggedExpression() {
        super();
        this.caption = "";
    }

    /**
     * Copy constructor. Be careful, it only copies the subtree starting from this node.
     * This means, neither the parent nor the siblings are copied as well. Only the expression itself and all children.
     *
     * If you want to copy the entire parse tree, you must copy the root of the tree!
     *
     * @param ppte a previously valid printable PoM expression.
     */
    public PrintablePomTaggedExpression( PrintablePomTaggedExpression ppte ) {
        super(MathTermUtility.secureClone(ppte.getRoot()), ppte.getTag(), ppte.getSecondaryTags());
        this.caption = ppte.caption;
        ppte.getNamedFeatures().forEach(super::addNamedFeature);
        for ( PrintablePomTaggedExpression child : ppte.getPrintableComponents() ) {
            PrintablePomTaggedExpression childCopy = new PrintablePomTaggedExpression(child);
            super.addComponent(childCopy);
        }
    }

    /**
     * Constructs a single node instance of a printable {@link PomTaggedExpression} with the given
     * {@link MathTerm} and tags. The tags can be empty.
     * @param mathTerm the math term for this node
     * @param exprTags the expression tags attached to this node
     */
    public PrintablePomTaggedExpression( MathTerm mathTerm, String... exprTags ) {
        super(mathTerm, exprTags);
        this.caption = mathTerm.getTermText();
    }

    /**
     * Constructs a printable {@link PomTaggedExpression} that was generated with the given string.
     *
     * If you wish get an instance of this class, you can simply use
     * {@link gov.nist.drmf.interpreter.pom.SemanticMLPWrapper#parse(String)} or
     * {@link gov.nist.drmf.interpreter.pom.MLPWrapper#parse(String)} or one of the similar methods.
     *
     * @param pte the {@link PomTaggedExpression} that was generated from {@param expr}
     * @param expr the expression that was used to generate the parse tree {@param pte}
     */
    public PrintablePomTaggedExpression( PomTaggedExpression pte, String expr ) {
        super();
        super.setRoot(pte.getRoot());
        super.setTag(pte.getTag());
        super.setSecondaryTags(pte.getSecondaryTags());
        for (String k : pte.getNamedFeatures().keySet())
            super.addNamedFeature(k, pte.getFeatureValue(k));

        // the fun part, every node has it's own caption
        this.caption = PomTaggedExpressionUtility.getNormalizedCaption(pte, expr);
        if ( PomTaggedExpressionUtility.isTeXEnvironment(pte) ) expr = TeXPreProcessor.removeTeXEnvironment(expr);

        // now we have to add the components and their respective substrings...
        for (PomTaggedExpression component : pte.getComponents()) {
            String innerExpression;

            IndexRange range = rangeCalculator.getRange(component, expr);

            innerExpression = expr.substring(range.getStart(), range.getEnd());
            expr = expr.substring(range.getEnd());

            PrintablePomTaggedExpression ppte = new PrintablePomTaggedExpression(component, innerExpression);
            super.addComponent(ppte);
        }
    }

    /**
     * Clears all components, its a single node now.
     */
    public void clearComponents(){
        super.getComponents().clear();
        this.caption = this.getRoot().getTermText();
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
        MatchablePomTaggedExpression m = PomMatcherBuilder.compile(this, "");
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
        checkComponentValidity( (List<PomTaggedExpression>) components );
        super.setComponents( (List<PomTaggedExpression>) components );
        populatingStringChanges();
    }

    /**
     * Provides an adapter for the {@link #setComponents(PomTaggedExpression...)} )} method, which only
     * allows components of type {@link PomTaggedExpression} rather. It throws an error if any
     * element is not of type {@link PrintablePomTaggedExpression}.
     * @param components a list of components.
     */
    public void setPrintableComponents(PomTaggedExpression... components) throws IllegalArgumentException {
        this.setPrintableComponents(Arrays.asList(components));
    }

    /**
     * This method throws an exception if the given components are not {@link PrintablePomTaggedExpression}.
     * @deprecated use {@link #setPrintableComponents(List)} instead.
     */
    @Override
    @Deprecated
    public void setComponents(List<PomTaggedExpression> components) throws IllegalArgumentException {
        checkComponentValidity(components);
        super.setComponents(components);
        populatingStringChanges();
    }

    /**
     * This method throws an exception if the given components are not {@link PrintablePomTaggedExpression}.
     * @deprecated use {@link #setPrintableComponents(List)} instead.
     */
    @Override
    @Deprecated
    public void setComponents(PomTaggedExpression... components) throws IllegalArgumentException {
        this.setComponents( Arrays.asList(components) );
    }

    /**
     * Returns an unmodifiable list of components. Hence it is no longer possible
     * to manipulate the components without using this class.
     * @return an unmodifiable list of components.
     */
    @Override
    public List<PomTaggedExpression> getComponents() {
        return Collections.unmodifiableList(super.getComponents());
    }

    @Override
    public boolean addComponent(PomTaggedExpression pte) throws IllegalArgumentException {
        checkComponentValidity(pte);
        boolean res = super.addComponent(pte);
        if (res) {
            populatingStringChanges();
        }
        return res;
    }

    @Override
    public boolean addComponent(int i, PomTaggedExpression pte) throws IllegalArgumentException {
        checkComponentValidity(pte);
        boolean res = super.addComponent(i, pte);
        if (res) {
            populatingStringChanges();
        }
        return res;
    }

    @Override
    public void addComponent(int i, MathTerm term) throws IllegalArgumentException {
        PrintablePomTaggedExpression pte = new PrintablePomTaggedExpression(term);
        this.addComponent(i, pte);
    }

    @Override
    public void set(PomTaggedExpression pte) throws IllegalArgumentException {
        checkComponentValidity(pte);
        super.set(pte);
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

    public void refreshTexComponents() {
        IndexRange range = rangeCalculator.getRange( this, caption );
        this.replaceCaption( caption.substring(range.getStart(), range.getEnd()) );
        this.populatingStringChanges();
    }

    @Override
    public void setRoot(MathTerm mathTerm) {
        String newCaption = PrintablePomTaggedExpressionUtility.getInternalNodeCommand(mathTerm);
        replaceCaption(newCaption);
        if ( this.getParent() != null ) {
            PrintablePomTaggedExpression parent = (PrintablePomTaggedExpression) this.getParent();
            parent.populatingStringChanges();
        }

        for ( PrintablePomTaggedExpression ppte : getPrintableComponents() ){
            this.caption += ppte.caption;
        }

        super.setRoot(mathTerm);
    }

    private void replaceCaption(String newCaption) {
        if ( TeXPreProcessor.wrappedInCurlyBrackets(caption) && !TeXPreProcessor.wrappedInCurlyBrackets(newCaption) ){
            String start = caption.substring(0,1);
            String end = caption.substring(caption.length()-1);
            this.caption = start + newCaption + end;
        } else this.caption = newCaption;
    }

    /**
     * Populates string changes from here onwards to the root of the tree.
     * It uses {@link PrintablePomTaggedExpressionUtility#getCaptionOfPPTEs(String, List)} method to build
     * the new caption of this node.
     */
    private void populatingStringChanges() {
        if ( !hasNoChildren() ) {
            String newCaption = PrintablePomTaggedExpressionUtility.getCaptionOfPPTEs(
                    PrintablePomTaggedExpressionUtility.getInternalNodeCommand(this),
                    getPrintableComponents()
            );
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
        return (List<PrintablePomTaggedExpression>)(List<?>) this.getComponents();
    }

    /**
     * Wraps the current caption in curly brackets, if it is not wrapped in curly brackets already
     */
    public void makeBalancedTexString() {
        if ( TeXPreProcessor.wrappedInCurlyBrackets(caption) ) return;
        caption = "{" + caption + "}";
        populatingStringChanges();
    }

    public void makeBalancedOptionalArgumentString() {
        caption = "[" + caption + "]";
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

    /**
     * Returns the root string representation of this element. For example, if this element is <code>z</code>
     * but part of <code>z+x</code>, this method returns <code>z+x</code> and not <code>z</code>.
     * @return the string representation of the root of the parse tree that this node belongs to.
     *         It is identical to {@link #getTexString()} if this element is the root element.
     */
    public String getRootTexString() {
        if ( this.getParent() != null ) return ((PrintablePomTaggedExpression) this.getParent()).getRootTexString();
        else return getTexString();
    }
}
