package gov.nist.drmf.interpreter.pom.extensions;

import org.intellij.lang.annotations.Language;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Andre Greiner-Petter
 */
public class MatcherConfig {
    @Language("RegExp")
    public static final String DEFAULT_ILLEGAL_TOKEN_FOR_WILDCARD = "[,;.]";

    @Language("RegExp")
    public static final String ALLOW_COMMA_IN_WILDCARD = "[;.]";

    private static final String ALL_KEY = "GENERIC_ALL_KEY";

    private static final Pattern NORM_KEY_PATTERN = Pattern.compile("(.*)(?:\\d+|N)$");

    private boolean allowLeadingTokens = false;
    private boolean allowFollowingTokens = false;
    private boolean ignoreBracketLogic = false;
    private boolean ignoreNumberOfAts = false;

    private final Map<String, String> illegalTokenForWildcards = new HashMap<>();

    private MatcherConfig() {
        this.illegalTokenForWildcards.put(ALL_KEY, DEFAULT_ILLEGAL_TOKEN_FOR_WILDCARD);
    }

    public boolean allowLeadingTokens() {
        return allowLeadingTokens;
    }

    public MatcherConfig allowLeadingTokens(boolean allowLeadingTokens) {
        this.allowLeadingTokens = allowLeadingTokens;
        return this;
    }

    public boolean allowFollowingTokens() {
        return allowFollowingTokens;
    }

    public MatcherConfig allowFollowingTokens(boolean allowFollowingTokens) {
        this.allowFollowingTokens = allowFollowingTokens;
        return this;
    }

    public boolean ignoreBracketLogic() {
        return ignoreBracketLogic;
    }

    public MatcherConfig ignoreBracketLogic(boolean ignoreBracketLogic) {
        this.ignoreBracketLogic = ignoreBracketLogic;
        return this;
    }

    public boolean ignoreNumberOfAts() {
        return ignoreNumberOfAts;
    }

    public MatcherConfig ignoreNumberOfAts(boolean ignoreNumberOfAts) {
        this.ignoreNumberOfAts = ignoreNumberOfAts;
        return this;
    }

    public @Language("RegExp") String getDefaultIllegalTokenForWildcards() {
        return this.illegalTokenForWildcards.getOrDefault(ALL_KEY, DEFAULT_ILLEGAL_TOKEN_FOR_WILDCARD);
    }

    public @Language("RegExp") String getIllegalTokensForWildcard(String wildcard) {
        if ( wildcard == null ) return getDefaultIllegalTokenForWildcards();

        if ( !this.illegalTokenForWildcards.containsKey(wildcard) ) {
            Matcher m = NORM_KEY_PATTERN.matcher(wildcard);
            if ( m.matches() ) {
                return getIllegalTokensForWildcard(m.group(1));
            }
        }

        return this.illegalTokenForWildcards.getOrDefault(wildcard, getDefaultIllegalTokenForWildcards());
    }

    public MatcherConfig allowCommaForAllWildcards() {
        return allowCommaForWildcard(ALL_KEY);
    }

    public MatcherConfig allowCommaForWildcard(String wildcard) {
        return setIllegalCharacterForWildcard(wildcard, ALLOW_COMMA_IN_WILDCARD);
    }

    public MatcherConfig setDefaultIllegalCharacterForWildcards(@Language("RegExp") String illegalCharacters) {
        return setIllegalCharacterForWildcard(ALL_KEY, illegalCharacters);
    }

    /**
     * Sets an illegal character pattern for the given wildcard. For example, if you do not want to allow
     * dots in the wildcard named 'var', you can set {@code setIllegalCharacterForWildcard("var", "[.]")}.
     *
     * You are also allowed to set a value for groups of wildcards. If you have 'var1', 'var2', 'varN', 'num'
     * as wildcards and want to define a set for all "var.." wildcards but not for "num" you can simply set
     * the value for 'var'. If the library is not able to find a specific rule for 'var1' it searches for 'var' instead.
     *
     * The fallback rule clips following digits or a capital 'N'. Hence, the fallback would not work for example
     * for 'varQ'.
     *
     * @param wildcard the name of the wildcard
     * @param illegalCharacters the set of illegal character as regex
     * @return this modified config object
     */
    public MatcherConfig setIllegalCharacterForWildcard(String wildcard, @Language("RegExp") String illegalCharacters) {
        this.illegalTokenForWildcards.put(wildcard, illegalCharacters);
        return this;
    }

    /**
     * The default config does not allow in-place matches (leading and following tokens of match is allowed) and
     * the bracket logic is enforced (must match) but the number of @s is ignored.
     * @return the default matcher.
     */
    public static MatcherConfig getDefaultMatchConfig() {
        return new MatcherConfig()
                .allowLeadingTokens(false)
                .allowFollowingTokens(false)
                .ignoreBracketLogic(false)
                .ignoreNumberOfAts(true);
    }

    public static MatcherConfig getAllowAllMatchConfig() {
        return new MatcherConfig()
                .allowLeadingTokens(true)
                .allowFollowingTokens(true)
                .ignoreBracketLogic(true)
                .ignoreNumberOfAts(true);
    }

    public static MatcherConfig getInPlaceMatchConfig() {
        return new MatcherConfig()
                .allowLeadingTokens(true)
                .allowFollowingTokens(true)
                .ignoreBracketLogic(false)
                .ignoreNumberOfAts(true);
    }

    public static MatcherConfig getExactMatchConfig() {
        return new MatcherConfig()
                .allowLeadingTokens(false)
                .allowFollowingTokens(false)
                .ignoreBracketLogic(false)
                .ignoreNumberOfAts(false);
    }
}
