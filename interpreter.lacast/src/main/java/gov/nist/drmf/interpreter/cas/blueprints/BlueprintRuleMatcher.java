package gov.nist.drmf.interpreter.cas.blueprints;

import gov.nist.drmf.interpreter.cas.translation.SemanticLatexTranslator;
import gov.nist.drmf.interpreter.common.latex.TeXPreProcessor;
import gov.nist.drmf.interpreter.common.interfaces.IBlueprintMatcher;
import gov.nist.drmf.interpreter.pom.common.FakeMLPGenerator;
import gov.nist.drmf.interpreter.pom.SemanticMLPWrapper;
import gov.nist.drmf.interpreter.pom.extensions.MatchablePomTaggedExpression;
import gov.nist.drmf.interpreter.pom.extensions.MatcherConfig;
import gov.nist.drmf.interpreter.pom.extensions.PomMatcherBuilder;
import gov.nist.drmf.interpreter.pom.extensions.PrintablePomTaggedExpression;
import mlp.ParseException;
import mlp.PomTaggedExpression;
import org.intellij.lang.annotations.Language;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Andre Greiner-Petter
 */
public class BlueprintRuleMatcher implements IBlueprintMatcher {
    @Language("RegExp")
    private static final String WILDCARD_PATTERN = "num[LU]\\d+|var(?:N|\\d+)";

    @Language("RegExp")
    private static final String DEFAULT_ILLEGAL_CHAR_FOR_VARS = "[;.=\\d]";

    public static final String VAR_TOKEN = "var";
    public static final String LOWER_BOUND_TOKEN = "numL";
    public static final String UPPER_BOUND_TOKEN = "numU";

    private static final String VAR_SPLITTER = " / ";
    private static final String LOW_UP_SPLITTER = ",";

    private final MatcherConfig matcherConfig;

    private List<String> vars = null;

    private String varPattern, upBPattern, lowBPattern;
    private final Pattern LIMIT_PATTERN;

    private final MatchablePomTaggedExpression matchablePom;
    private final SemanticLatexTranslator translator;
    private final String[] limitPattern;

    private Map<String, String> matches;

    private boolean isOverSet;

    private final String pattern;

    public BlueprintRuleMatcher(SemanticLatexTranslator translator, String pattern, String replacement) throws ParseException {
        PrintablePomTaggedExpression ppte = SemanticMLPWrapper.getStandardInstance().parse(pattern);
        ppte = (PrintablePomTaggedExpression) FakeMLPGenerator.wrapNonSequenceInSequence(ppte);
        matchablePom = PomMatcherBuilder.compile(ppte, WILDCARD_PATTERN);
        this.translator = translator;
        this.limitPattern = replacement.split(VAR_SPLITTER);
        this.LIMIT_PATTERN = setupPattern();
        this.pattern = pattern;
        this.matcherConfig = MatcherConfig.getExactMatchConfig()
                .allowLeadingTokens(false)
                .allowFollowingTokens(false)
                .setIllegalCharacterForWildcard("var", DEFAULT_ILLEGAL_CHAR_FOR_VARS)
                .setIllegalCharacterForWildcard("varN", DEFAULT_ILLEGAL_CHAR_FOR_VARS);
    }

    private Pattern setupPattern() {
        lowBPattern = translate(LOWER_BOUND_TOKEN);
        upBPattern = translate(UPPER_BOUND_TOKEN);
        varPattern = translate(VAR_TOKEN);

//        lowBPattern = lowBPattern.replaceAll("\\*", "\\\\*");
//        upBPattern = upBPattern.replaceAll("\\*", "\\\\*");
//        varPattern = varPattern.replaceAll("\\*", "\\\\*");

        lowBPattern = Pattern.quote(lowBPattern);
        upBPattern = Pattern.quote(upBPattern);
        varPattern = Pattern.quote(varPattern);

        return Pattern.compile(
                "("+lowBPattern+"|"+varPattern+"|"+upBPattern+").?(\\d+)"
        );
    }

    @Override
    public boolean match(String expression) {
        if ( expression == null || expression.isBlank() ) return false;
        expression = preCleaning(expression);
        try {
            PrintablePomTaggedExpression ppte = SemanticMLPWrapper.getStandardInstance().parse(expression);
            return match(ppte);
        } catch (ParseException e) {
            return false;
        }

    }

    public boolean match(PomTaggedExpression... expressions) {
        this.isOverSet = false;
        if ( expressions == null || expressions.length == 0 ) return false;
        boolean match = false;
        if ( expressions.length > 1 ) {
            PrintablePomTaggedExpression ppte = FakeMLPGenerator.generateEmptySequencePPTE();
            ppte.setPrintableComponents(expressions);
            match = matchablePom.match(ppte, matcherConfig);
            this.isOverSet = ppte.getTexString().matches(".*\\\\(in|divides)[^A-Za-z]+.*");
        } else {
            PrintablePomTaggedExpression ppte = (PrintablePomTaggedExpression) FakeMLPGenerator.wrapNonSequenceInSequence(expressions[0]);
            match = matchablePom.match(ppte, matcherConfig);
            this.isOverSet = ppte.getTexString().matches(".*\\\\(in|divides)[^A-Za-z]+.*");
        }
        if ( match ) {
            analyzeMatchedGroups();
        }
        return match;
    }

    private void analyzeMatchedGroups() {
        matches = matchablePom.getStringMatches();

        Collection<Match> varsCol = new LinkedList<>();

        matches.forEach((key, value) -> {
            if ( key.startsWith("var") ) {
                if ( key.endsWith("N") ) {
                    String[] values = value.split(",");
                    for ( int i = 0; i < values.length; i++ ) {
                        varsCol.add(new Match(i, values[i]));
                    }
                } else varsCol.add(new Match(key.substring(3), value));
            }
        });

        this.vars = parseToList(varsCol);
    }

    private List<String> parseToList(Collection<Match> matches) {
        return matches.stream()
                .sorted(Comparator.comparingInt(Match::position))
                .map( m -> translator.translate(m.value))
                .collect(Collectors.toList());
    }

    protected MathematicalEssentialOperatorMetadata getExtractedMEOM() {
        LinkedList<String> lowers = new LinkedList<>();
        LinkedList<String> uppers = new LinkedList<>();

        for ( int i = 0; i < this.vars.size(); i++ ) {
            String[] lu;
            if ( i >= limitPattern.length ) {
                lu = limitPattern[limitPattern.length-1].split(LOW_UP_SPLITTER);
            } else {
                lu = limitPattern[i].split(LOW_UP_SPLITTER);
            }

            String t = translate(lu[0]);
            Matcher lMatcher = LIMIT_PATTERN.matcher(t); // lower limit
            lowers.addLast(replaceAllPatterns(lMatcher));

            if ( lu.length == 2 ) {
                t = translate(lu[1]);
                Matcher uMatcher = LIMIT_PATTERN.matcher(t); // upper limit
                uppers.addLast(replaceAllPatterns(uMatcher));
            } else {
                uppers.addLast(translate(MathematicalEssentialOperatorMetadata.DEFAULT_UPPER_LIMIT));
            }
        }

        MathematicalEssentialOperatorMetadata l = new MathematicalEssentialOperatorMetadata(
                this.vars,
                lowers,
                uppers
        );
        l.setLimitOverSet(isOverSet);
        return l;
    }

    private String translate(String str) {
        translator.translate(str);
        return translator.getTranslatedExpression();
    }

    private String replaceAllPatterns( Matcher matcher ) {
        StringBuffer buffer = new StringBuffer();
        while( matcher.find() ){
            if ( matcher.group(1).matches(lowBPattern) ) {
                // lower index replacement
                int idx = Integer.parseInt(matcher.group(2));
                String trans = translator.translate( matches.getOrDefault("numL"+idx, "" ));
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(trans));
            } else if ( matcher.group(1).matches(upBPattern) ) {
                // lower index replacement
                int idx = Integer.parseInt(matcher.group(2));
                String trans = translator.translate( matches.getOrDefault("numU"+idx, "" ));
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(trans));
            } else if ( matcher.group(1).matches(varPattern) ) {
                // lower index replacement
                int idx = Integer.parseInt(matcher.group(2));
                String trans = this.vars.get(idx-1);
                matcher.appendReplacement(buffer, trans);
            }
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    public static String preCleaning(String constraint){
        constraint = TeXPreProcessor.preProcessingTeX(constraint);
        constraint = constraint.replaceAll("\\\\[lc]?dots[cbmio]?", "\\\\dots");
        constraint = constraint.replaceAll("([^,\\s])\\s*\\\\dots", "$1, \\\\dots");
        constraint = constraint.replaceAll("\\\\[it]?frac", "\\\\frac");
        constraint = constraint.replaceAll("\\\\(ne|le|ge)([^a-zA-Z])", "\\\\$1q$2");
        constraint = constraint.replaceAll("[\\s,;.]*$", "");
        constraint = constraint.replaceAll("@*", "");
        return constraint;
    }

    private static class Match {
        final int pos;
        final String value;

        Match(String pos, String value) {
            this(Integer.parseInt(pos), value);
        }

        Match(int pos, String value) {
            this.pos = pos;
            this.value = value;
        }

        int position() {
            return pos;
        }
    }
}
