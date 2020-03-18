package gov.nist.drmf.interpreter.mlp.extensions;

import gov.nist.drmf.interpreter.common.exceptions.NotMatchableException;
import gov.nist.drmf.interpreter.common.interfaces.IMatcher;
import mlp.MathTerm;
import mlp.PomTaggedExpression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * This object is essentially an extension of the classic PomTaggedExpression.
 * However, it is matchable. That means, this object is tree-like class that supports
 * wildcards. Another {@link PomTaggedExpression} may matches this object or may not match it,
 * depending on the wildcards.
 *
 * @author Andre Greiner-Petter
 */
public class ComparablePomTaggedExpression extends PomTaggedExpression implements IMatcher<PomTaggedExpression> {
    private static final Logger LOG = LogManager.getLogger(ComparablePomTaggedExpression.class.getName());

    /**
     * If this node is a wildcard or not
     */
    private boolean isWildcard = false;

    /**
     * The wildcard ID
     */
    private String wildcardID;

    /**
     * If this node is a wildcard and another expression matches this class,
     * the wildcard matches a specific subexpression, given here.
     */
    private PomTaggedExpression wildcardMatch = null;

    /**
     * Essentially a copy of {@link PomTaggedExpression#getComponents()}
     */
    private List<ComparablePomTaggedExpression> components;

    /**
     * The parent node of this element. It is null, if this element is the root.
     */
    private ComparablePomTaggedExpression parent;

    /**
     * Previous sibling, if any
     */
    private ComparablePomTaggedExpression previousSibling;

    /**
     * Next sibling, if any
     */
    private ComparablePomTaggedExpression nextSibling;

    /**
     * Copy constructor to extend a {@link PomTaggedExpression} and its components to
     * a {@link ComparablePomTaggedExpression}.
     * @param refRoot the reference {@link PomTaggedExpression}
     * @param wildcardPattern a regex that defines the set of wildcards
     */
    public ComparablePomTaggedExpression(PomTaggedExpression refRoot, String wildcardPattern) {
        this(null, refRoot, wildcardPattern);
        this.previousSibling = null;
        this.nextSibling = null;
    }

    /**
     * Copy constructor to extend
     * @param parent parent node (null if this element is the root9
     * @param refRoot the reference {@link PomTaggedExpression}
     * @param wildcardPattern a regex that defines the set of wildcards
     */
    private ComparablePomTaggedExpression(ComparablePomTaggedExpression parent, PomTaggedExpression refRoot, String wildcardPattern) {
        super(refRoot.getRoot(), refRoot.getTag(), refRoot.getSecondaryTags());
        Map<String, String> refFeatures = refRoot.getNamedFeatures();
        for ( String k : refFeatures.keySet() )
            super.setNamedFeature(k, refFeatures.get(k));

        this.parent = parent;
        this.wildcardMatch = null;

        String text = refRoot.getRoot().getTermText();
        if ( text.matches(wildcardPattern) ) {
            if ( !refRoot.getComponents().isEmpty() )
                throw new NotMatchableException("A wildcard node cannot have children.");
            this.isWildcard = true;
            this.wildcardID = text;
        } else {
            this.isWildcard = false;
            this.wildcardID = null;
        }

        List<PomTaggedExpression> comps = refRoot.getComponents();
        this.components = new LinkedList<>();

        ComparablePomTaggedExpression prevNode = null;
        for ( PomTaggedExpression pte : comps ) {
            ComparablePomTaggedExpression cpte = new ComparablePomTaggedExpression(this, pte, wildcardPattern);
            super.addComponent(cpte);
            this.components.add(cpte);

            // define previous and next element
            this.setPreviousSibling(prevNode);
            if ( prevNode != null ) {
                prevNode.setNextSibling(cpte);
                if ( prevNode.isWildcard && cpte.isWildcard )
                    throw new NotMatchableException("Two consecutive wildcards may have no unique matches.");
            }
            prevNode = cpte;
        }
    }

    private void setNextSibling(ComparablePomTaggedExpression nextSibling) {
        this.nextSibling = nextSibling;
    }

    private void setPreviousSibling(ComparablePomTaggedExpression previousSibling) {
        this.previousSibling = previousSibling;
    }

    /**
     * Resets the previous matches, if any
     */
    private void depthResetMatches() {
        this.wildcardMatch = null;
        for ( ComparablePomTaggedExpression cpte : components ) {
            cpte.depthResetMatches();
        }
    }

    @Override
    public boolean match(PomTaggedExpression expression) {
        depthResetMatches();
        return match(expression, new LinkedList<>());
    }

    /**
     *
     * @param expression
     * @param followingExpressions
     * @return
     */
    private boolean match(PomTaggedExpression expression, List<PomTaggedExpression> followingExpressions) {
        if ( expression instanceof ComparablePomTaggedExpression ) {
            throw new NotMatchableException("Cannot compare a blueprint POM " +
                    "element with another blueprint POM element.");
        }

        // essentially there are two cases, either it is not a wildcard, that it must match directly the reference
        if ( !isWildcard ) {
            MathTerm otherRoot = expression.getRoot();
            MathTerm thisRoot = getRoot();

            // TODO might be too strict
            if ( !thisRoot.getTermText().equals(otherRoot.getTermText()) ) {
                return false;
            }

            // since both term matches, we have to check their children
            // if this object doesn't have children, we can straight check the match
            if ( this.components.isEmpty() ) return expression.getComponents().isEmpty();

//            try {
                // the complex case, check if all children matches
//                PomTaggedExpression cloneRef = expression.clone();
                List<PomTaggedExpression> refComponents = expression.getComponents();

                int idx = 0;
                while ( !refComponents.isEmpty() ) {
                    PomTaggedExpression firstRef = refComponents.remove(0);
                    ComparablePomTaggedExpression matcherElement = components.get(idx);

                    if ( !matcherElement.match(firstRef, refComponents) ) return false;

                    idx++;
                }

                // now the reference elements are empty... so we either reached the end (=> match) or not (=> no match)
                return idx == components.size();
//            } catch (CloneNotSupportedException e) {
//                throw new NotMatchableException(e);
//            }
        } else {
            // or it is a wildcard, which means it can be essentially anything
            // note that a wildcard cannot have any children, which makes it easier

            // now we match everything until the next element (no wildcard) is reached
            PomTaggedExpression tmpParent = FakeMLPGenerator.generateEmptySequencePTE();

            // if there is no next element, the entire rest matches this wildcard
            if ( nextSibling == null ) {
                tmpParent.addComponent(expression);
                while ( !followingExpressions.isEmpty() )
                    tmpParent.addComponent(followingExpressions.remove(0));
                this.wildcardMatch = tmpParent;
                return true;
            }

            // otherwise, add elements, until the next element matches
            if ( followingExpressions.isEmpty() ) return false;

            tmpParent.addComponent(expression);
            PomTaggedExpression next = followingExpressions.remove(0);

            while ( !nextSibling.match(next, followingExpressions) ) {
                if ( followingExpressions.isEmpty() )
                    return false;
                next = followingExpressions.remove(0);
                tmpParent.addComponent(next);
            }

            // nextSibling has matched the next element in followingExpression... so put add back into the queue
            // and return true
            followingExpressions.add(0, next);
            this.wildcardMatch = tmpParent;
            return true;
        }
    }

    public Map<String, PomTaggedExpression> getMatches() {
        Map<String, PomTaggedExpression> matches = new HashMap<>();

        if ( this.wildcardMatch != null ) matches.put(this.wildcardID, wildcardMatch);

        for ( ComparablePomTaggedExpression cpte : components ) {
            matches.putAll( cpte.getMatches() );
        }

        return matches;
    }
}
