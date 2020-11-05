package gov.nist.drmf.interpreter.pom.moi;

import gov.nist.drmf.interpreter.common.exceptions.NotMatchableException;
import gov.nist.drmf.interpreter.common.grammar.MathTermTags;
import gov.nist.drmf.interpreter.pom.SemanticMLPWrapper;
import gov.nist.drmf.interpreter.pom.extensions.*;
import mlp.MathTerm;
import mlp.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.intellij.lang.annotations.Language;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Andre Greiner-Petter
 */
public class MathematicalObjectOfInterest {
    private static final Logger LOG = LogManager.getLogger(MathematicalObjectOfInterest.class.getName());

    @Language("RegExp")
    public static final String WILDCARD_PATTERN = "var\\d+";

    private static final SemanticMLPWrapper mlp = SemanticMLPWrapper.getStandardInstance();

    private final Set<String> identifiers;
    private final PrintablePomTaggedExpression moi;
    private MatchablePomTaggedExpression matchableMOI;

    private String originalLaTeX;
    private String pattern;

    private HashMap<String, String> wildcardIdentifierMapping;
    private HashMap<String, String> potentialPrimaryIdentifierWildcardMapping;

    /**
     * Keep Kryo happy for serialization
     */
    private MathematicalObjectOfInterest() {
        this.identifiers = null;
        this.moi = null;
        this.matchableMOI = null;
    }

    public MathematicalObjectOfInterest(String latex) throws ParseException {
        this(mlp.parse(latex));
    }

    public MathematicalObjectOfInterest(PrintablePomTaggedExpression moi) {
        this.originalLaTeX = moi.getTexString();
        this.moi = moi;

        PrintablePomTaggedExpression moiCopy = new PrintablePomTaggedExpression(moi);
        Collection<PrintablePomTaggedExpression> identifierNodes = PrintablePomTaggedExpressionUtility.getIdentifierNodes(moiCopy);

        this.identifiers = identifierNodes.stream()
                .map( PrintablePomTaggedExpression::getRoot )
                .map( MathTerm::getTermText )
                .collect(Collectors.toSet());

        if ( this.identifiers.size() > 1 ) {
            this.wildcardIdentifierMapping = replaceIdentifiersByWildcards(identifierNodes);
        } else this.wildcardIdentifierMapping = new HashMap<>();

        this.pattern = moiCopy.getTexString();
        LOG.debug("Generated MOI pattern: " + pattern);
        try {
            this.matchableMOI = PomMatcherBuilder.compile(moiCopy, WILDCARD_PATTERN);
        } catch ( NotMatchableException nme ) {
            LOG.warn("Node cannot be generated, because the wildcard expression is not matchable: " + nme.toString());
            this.matchableMOI = null;
        }

        this.potentialPrimaryIdentifierWildcardMapping = new HashMap<>();
        for (Map.Entry<String, String> wildcardIdentifier : this.wildcardIdentifierMapping.entrySet() ) {
            if ( !this.originalLaTeX.matches(".*[(\\[|]\\s*\\Q"+wildcardIdentifier.getValue()+"\\E\\s*(?:right)?[|\\])].*")  ) {
                // identifier does not appear isolated in parentheses, so its a candidate for primary identifier
                this.potentialPrimaryIdentifierWildcardMapping.put(wildcardIdentifier.getKey(), wildcardIdentifier.getValue());
            }
        }
    }

    private HashMap<String, String> replaceIdentifiersByWildcards(
            Collection<PrintablePomTaggedExpression> identifierNodes
    ) {
        HashMap<String, String> wildcardToIdentifierMap = new HashMap<>();
        HashMap<String, String> wildcardMemoryMap = new HashMap<>();
        for ( PrintablePomTaggedExpression identifierNode : identifierNodes ) {
            String wildcard = wildcardMemoryMap.computeIfAbsent(
                    identifierNode.getRoot().getTermText(),
                    key -> "var"+wildcardMemoryMap.size()
            );
            wildcardToIdentifierMap.put(wildcard, identifierNode.getRoot().getTermText());
            identifierNode.setRoot(new MathTerm(wildcard, MathTermTags.alphanumeric.tag()));
        }
        return wildcardToIdentifierMap;
    }

    /**
     * Returns either null (if no match was found) or a dependency pattern that represents the match.
     * @param expression the MOI that should be matched
     * @return the dependency pattern that matched or null of no match was found
     */
    public DependencyPattern match(MathematicalObjectOfInterest expression) {
        if ( Objects.isNull(expression) || Objects.isNull(matchableMOI) || Objects.isNull(expression.matchableMOI) )
            return null;

        PomMatcher matcher = this.matchableMOI.matcher(
                expression.moi,
                MatcherConfig.getInPlaceMatchConfig().ignoreNumberOfAts(false)
        );

        while ( matcher.find() ) {
            // we found a match. let's see if at least one primary identifier is shared
            Map<String, String> groups = matcher.groups();

            if ( groups.isEmpty() ) {
                // if there are no wildcards, it must have been a perfect match, e.g., z is in \Gamma(z).
                return new DependencyPattern(this.pattern, matcher);
            }

            for ( String wildcard : groups.keySet() ) {
                // if at least one identifier is also a potential primary identifier, we found a match and return true
                if ( groups.get(wildcard).equals( this.potentialPrimaryIdentifierWildcardMapping.get(wildcard) ) )
                    return new DependencyPattern(this.pattern, matcher);
            }
        }

        // we did not found appropriate match
        return null;
    }

    public String getOriginalLaTeX() {
        return originalLaTeX;
    }

    public String getPattern() {
        return pattern;
    }

    public Set<String> getIdentifiers() {
        return identifiers;
    }

    public PrintablePomTaggedExpression getMoi() {
        return moi;
    }

    public MatchablePomTaggedExpression getMatcher() {
        return matchableMOI;
    }

    public HashMap<String, String> getWildcardIdentifierMapping() {
        return wildcardIdentifierMapping;
    }

    public HashMap<String, String> getPotentialPrimaryIdentifierWildcardMapping() {
        return potentialPrimaryIdentifierWildcardMapping;
    }

    public boolean isIdentifier() {
        return this.identifiers.size() == 1 && this.identifiers.contains(this.originalLaTeX.trim());
    }
}
