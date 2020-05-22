package gov.nist.drmf.interpreter.mlp.extensions;

/**
 * @author Andre Greiner-Petter
 */
public class MatcherConfig {
    private boolean allowLeadingTokens = false;
    private boolean allowFollowingTokens = false;
    private boolean ignoreBracketLogic = false;

    private MatcherConfig() {}

    public MatcherConfig(boolean ignoreBracketLogic) {
        this(false, false, ignoreBracketLogic);
    }

    public MatcherConfig(boolean allowLeadingTokens, boolean allowFollowingTokens, boolean ignoreBracketLogic) {
        this.allowLeadingTokens = allowLeadingTokens;
        this.allowFollowingTokens = allowFollowingTokens;
        this.ignoreBracketLogic = ignoreBracketLogic;
    }

    public boolean allowLeadingTokens() {
        return allowLeadingTokens;
    }

    public void allowLeadingTokens(boolean allowLeadingTokens) {
        this.allowLeadingTokens = allowLeadingTokens;
    }

    public boolean allowFollowingTokens() {
        return allowFollowingTokens;
    }

    public void allowFollowingTokens(boolean allowFollowingTokens) {
        this.allowFollowingTokens = allowFollowingTokens;
    }

    public boolean ignoreBracketLogic() {
        return ignoreBracketLogic;
    }

    public void ignoreBracketLogic(boolean ignoreBracketLogic) {
        this.ignoreBracketLogic = ignoreBracketLogic;
    }

    public static MatcherConfig getInPlaceMatchConfig() {
        return new MatcherConfig(true, true, false);
    }

    public static MatcherConfig getExactMatchConfig() {
        return new MatcherConfig(false, false, false);
    }
}
