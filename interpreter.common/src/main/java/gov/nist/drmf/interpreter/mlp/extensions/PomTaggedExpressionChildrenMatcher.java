package gov.nist.drmf.interpreter.mlp.extensions;

import gov.nist.drmf.interpreter.mlp.PomTaggedExpressionUtility;
import mlp.MathTerm;
import mlp.PomTaggedExpression;

import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * This class should only be generated by {@link MatchablePomTaggedExpression} due
 * initialization. This class handles the children (components) of the given parent.
 * It also provide helpful functions to match child components.
 * @author Andre Greiner-Petter
 */
public class PomTaggedExpressionChildrenMatcher {
    /**
     * The reference parent.
     */
    private final AbstractMatchablePomTaggedExpression parent;

    /**
     * The children of the parent node
     */
    private final LinkedList<MatchablePomTaggedExpression> children;

    private MatchablePomTaggedExpression hiddenFirstElement;

    /**
     * Keep Kryo happy for serialization
     */
    private PomTaggedExpressionChildrenMatcher() {
        this(new MatchablePomTaggedExpression());
    }

    /**
     * @param parent the parent node
     */
    PomTaggedExpressionChildrenMatcher(
            AbstractMatchablePomTaggedExpression parent
    ) {
        this.parent = parent;
        this.children = new LinkedList<>();
        this.hiddenFirstElement = null;
    }

    /**
     * Adds a node the list of children.
     * It updates the provided parent node as well.
     * @param cpte new child
     */
    public void add( MatchablePomTaggedExpression cpte ) {
        this.parent.addComponent(cpte);
        this.children.add(cpte);
    }

    /**
     * @return true if the first child is a wildcard otherwise false.
     * It also returns false if there are no children added yet.
     */
    public boolean isFirstChildWildcard() {
        return hiddenFirstElement != null ||
                ( !this.children.isEmpty() && this.children.get(0).isWildcard() );
    }

    /**
     * Removes the first element from the defined parent and the children list.
     * @return the first element of the children
     * @throws NoSuchElementException if there are no children
     */
    public MatchablePomTaggedExpression hideFirstWildcard() throws NoSuchElementException {
        if ( hiddenFirstElement != null ) return hiddenFirstElement;
        // remove first element from parent node
        try {
            parent.getComponents().remove(0);
        } catch (IndexOutOfBoundsException ioobe) {
            throw new NoSuchElementException(ioobe.getMessage());
        }
        hiddenFirstElement = this.children.removeFirst();
        return hiddenFirstElement;
    }

    /**
     * @return true if this list of children is empty
     */
    public boolean isEmpty() {
        return this.children.isEmpty();
    }

    /**
     * @return the number of children
     */
    public int size() {
        return this.children.size();
    }

    /**
     * Matches children in a non-wildcard environment.
     * @param refComponents a copy of the children that must be matched.
     *                      This list will be manipulated, so do not provide
     *                      the original list of children to match.
     * @param config the matching configuration
     * @return true if it matches, false otherwise
     */
    public boolean matchNonWildCardChildren(
            LinkedList<PrintablePomTaggedExpression> refComponents,
            MatcherConfig config
    ) {
        int idx = 0;
        while (idx < size() && !refComponents.isEmpty()) {
            MatchablePomTaggedExpression matcherElement = this.children.get(idx);
            if ( config.ignoreNumberOfAts() && PomTaggedExpressionUtility.isAt(matcherElement) ) {
                idx++;
                continue;
            }

            PrintablePomTaggedExpression firstRef = refComponents.removeFirst();
            if (!matcherElement.match(firstRef, refComponents, config)) return false;

            idx++;
        }

        return validIndex(refComponents, config, idx);
    }

    private boolean validIndex(
            LinkedList<PrintablePomTaggedExpression> refComponents,
            MatcherConfig config,
            int idx
    ) {
        if ( !config.allowFollowingTokens() && idx == size() ) return refComponents.isEmpty();
        else return idx == size();
    }
}
