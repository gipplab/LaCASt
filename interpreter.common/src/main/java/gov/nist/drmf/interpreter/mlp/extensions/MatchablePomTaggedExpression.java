package gov.nist.drmf.interpreter.mlp.extensions;

import gov.nist.drmf.interpreter.common.exceptions.NotMatchableException;
import gov.nist.drmf.interpreter.common.grammar.Brackets;
import gov.nist.drmf.interpreter.common.interfaces.IMatcher;
import gov.nist.drmf.interpreter.mlp.MLPWrapper;
import mlp.MathTerm;
import mlp.ParseException;
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
public class MatchablePomTaggedExpression extends PomTaggedExpression implements IMatcher<PrintablePomTaggedExpression> {
    private static final Logger LOG = LogManager.getLogger(MatchablePomTaggedExpression.class.getName());

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
    private List<PrintablePomTaggedExpression> wildcardMatch;

    /**
     * Essentially a copy of {@link PomTaggedExpression#getComponents()}
     */
    private List<MatchablePomTaggedExpression> components;

    /**
     * Next sibling, if any
     */
    private MatchablePomTaggedExpression nextSibling;

    /**
     * For better performance, it is recommended to have one MLPWrapper object.
     * So if not necessary,
     *
     * @param mlp
     * @param expression
     * @param wildcardID
     * @throws ParseException
     */
    public MatchablePomTaggedExpression(MLPWrapper mlp, String expression, String wildcardID) throws ParseException {
        this(mlp.simpleParse(expression), wildcardID);
    }

    /**
     * @param expression
     * @param wildcardPattern
     * @throws ParseException
     */
    public MatchablePomTaggedExpression(String expression, String wildcardPattern) throws ParseException {
        this(MLPWrapper.getStandardInstance().simpleParse(expression), wildcardPattern);
    }

    /**
     * Copy constructor to extend a {@link PomTaggedExpression} and its components to
     * a {@link MatchablePomTaggedExpression}.
     *
     * @param refRoot         the reference {@link PomTaggedExpression}
     * @param wildcardPattern a regex that defines the set of wildcards
     */
    public MatchablePomTaggedExpression(PomTaggedExpression refRoot, String wildcardPattern) {
        super(refRoot.getRoot(), refRoot.getTag(), refRoot.getSecondaryTags());
        Map<String, String> refFeatures = refRoot.getNamedFeatures();
        for (String k : refFeatures.keySet())
            super.setNamedFeature(k, refFeatures.get(k));

        this.wildcardMatch = null;
        this.wildcardMatch = new LinkedList<>();

        String text = refRoot.getRoot().getTermText();
        if (text.matches(wildcardPattern)) {
            if (!refRoot.getComponents().isEmpty())
                throw new NotMatchableException("A wildcard node cannot have children.");
            this.isWildcard = true;
            this.wildcardID = text;
        } else {
            this.isWildcard = false;
            this.wildcardID = null;
        }

        List<PomTaggedExpression> comps = refRoot.getComponents();
        this.components = new LinkedList<>();

        MatchablePomTaggedExpression prevNode = null;
        for (PomTaggedExpression pte : comps) {
            MatchablePomTaggedExpression cpte = new MatchablePomTaggedExpression(pte, wildcardPattern);
            super.addComponent(cpte);
            this.components.add(cpte);

            if (prevNode != null) {
                prevNode.setNextSibling(cpte);
                if (prevNode.isWildcard && cpte.isWildcard)
                    throw new NotMatchableException("Two consecutive wildcards may have no unique matches.");
            }
            prevNode = cpte;
        }
    }

    private void setNextSibling(MatchablePomTaggedExpression nextSibling) {
        this.nextSibling = nextSibling;
    }

    /**
     * Resets the previous matches, if any
     */
    private void depthResetMatches() {
        this.wildcardMatch.clear();
        for (MatchablePomTaggedExpression cpte : components) {
            cpte.depthResetMatches();
        }
    }

    public boolean match(String expression) {
        try {
            PrintablePomTaggedExpression ppte = MLPWrapper.getStandardInstance().parse(expression);
            return match(ppte);
        } catch (ParseException e) {
            LOG.warn("Cannot parse the given expression " + expression);
            return false;
        }
    }

    @Override
    public boolean match(PrintablePomTaggedExpression expression) {
        depthResetMatches();
        expression = (PrintablePomTaggedExpression)MLPWrapper.normalize(expression);
        return match(expression, new LinkedList<>());
    }

    private boolean match(PrintablePomTaggedExpression expression, List<PrintablePomTaggedExpression> followingExpressions) {
        // essentially there are two cases, either it is not a wildcard, that it must match directly the reference
        if (!isWildcard) {
            MathTerm otherRoot = expression.getRoot();
            MathTerm thisRoot = getRoot();

            // TODO might be too strict
            if (!thisRoot.getTermText().equals(otherRoot.getTermText())) {
                return false;
            }

            // since both term matches, we have to check their children
            // if this object doesn't have children, we can straight check the match
            if (this.components.isEmpty()) return expression.getComponents().isEmpty();

            LinkedList<PrintablePomTaggedExpression> refComponents = new LinkedList<>(expression.getPrintableComponents());

            int idx = 0;
            while (!refComponents.isEmpty()) {
                PrintablePomTaggedExpression firstRef = refComponents.removeFirst();
                MatchablePomTaggedExpression matcherElement = components.get(idx);

                if (!matcherElement.match(firstRef, refComponents)) return false;

                idx++;
            }

            // now the reference elements are empty... so we either reached the end (=> match) or not (=> no match)
            return idx == components.size();
        } else {
            // or it is a wildcard, which means it can be essentially anything
            // note that a wildcard cannot have any children, which makes it easier

            // if there is no next element, the entire rest matches this wildcard
            if (nextSibling == null) {
                while (!followingExpressions.isEmpty())
                    this.wildcardMatch.add(followingExpressions.remove(0));
                return true;
            }

            // otherwise, add elements, until the next element matches
            if (followingExpressions.isEmpty()) return false;

            this.wildcardMatch.add(expression);
            PrintablePomTaggedExpression next = followingExpressions.remove(0);

            LinkedList<Brackets> bracketStack = new LinkedList<>();

            while (!bracketStack.isEmpty() || !nextSibling.match(next, followingExpressions)) {
                if (followingExpressions.isEmpty())
                    return false;

                if ( isNotAllowedTokenForWildcardMatch(next) )
                    return false;

                this.wildcardMatch.add(next);
                Brackets br = Brackets.getBracket( next.getRoot().getTermText() );
                if ( br != null ) {
                    if ( br.opened ) {
                        bracketStack.addLast(br);
                    } else if ( !bracketStack.isEmpty() ) {
                        if ( bracketStack.getLast().isCounterPart(br) )
                            bracketStack.removeLast();
                        else throw new NotMatchableException("Not matching parenthesis. Found " + br +
                                " but last opening was " + bracketStack.getLast());
                    } else {
                        throw new NotMatchableException(
                                "Not matching parenthesis. Found " + br +
                                        " but non was opened before.");
                    }
                }

                next = followingExpressions.remove(0);
            }

            // nextSibling has matched the next element in followingExpression... so put add back into the queue
            // and return true
            followingExpressions.add(0, next);
            return true;
        }
    }

    private boolean isNotAllowedTokenForWildcardMatch(PomTaggedExpression pte) {
        String mathTerm = pte.getRoot().getTermText();
        return mathTerm != null && mathTerm.matches("[,;.]");
    }

    public Map<String, String> getStringMatches() {
        Map<String, String> out = new HashMap<>();
        Map<String, List<PrintablePomTaggedExpression>> matches = getMatches();

        for (String key : matches.keySet()) {
            String str = PrintablePomTaggedExpression.buildString(matches.get(key));
            out.put(key, str);
        }

        return out;
    }

    public Map<String, List<PrintablePomTaggedExpression>> getMatches() {
        Map<String, List<PrintablePomTaggedExpression>> matches = new HashMap<>();

        if (this.isWildcard && !this.wildcardMatch.isEmpty())
            matches.put(this.wildcardID, Collections.unmodifiableList(wildcardMatch));

        for (MatchablePomTaggedExpression cpte : components) {
            matches.putAll(cpte.getMatches());
        }

        return matches;
    }
}
