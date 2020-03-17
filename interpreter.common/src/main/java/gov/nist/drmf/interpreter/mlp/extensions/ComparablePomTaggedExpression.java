package gov.nist.drmf.interpreter.mlp.extensions;

import mlp.PomTaggedExpression;

import java.util.LinkedList;
import java.util.List;

/**
 * This object is essentially an extension of the classic PomTaggedExpression.
 * However, it is matchable. That means, this object is tree-like class that supports
 * wildcards. Another {@link PomTaggedExpression} may matches this object or may not match it,
 * depending on the wildcards.
 *
 * @author Andre Greiner-Petter
 */
public class ComparablePomTaggedExpression extends PomTaggedExpression implements Comparable<PomTaggedExpression> {
    /**
     *
     */
    private boolean isWildcard = false;

    /**
     * Copy constructor to extend
     * @param parent
     */
    public ComparablePomTaggedExpression(PomTaggedExpression parent) {
        super(parent.getRoot());

        List<PomTaggedExpression> comps = parent.getComponents();

        for ( PomTaggedExpression pte : comps ) {
            ComparablePomTaggedExpression cpte = new ComparablePomTaggedExpression(pte);
            super.addComponent(cpte);
        }
    }

    @Override
    public int compareTo(PomTaggedExpression pomTaggedExpression) {
        return 0;
    }
}
