package gov.nist.drmf.interpreter.mlp.extensions;

/**
 * @author Andre Greiner-Petter
 */
public class MatcherConfig {
    private boolean allowFollowingTokens = false;
    private boolean ignoreBracketLogic = false;

    private MatcherConfig() {}

    public MatcherConfig(boolean allowFollowingTokens, boolean ignoreBracketLogic) {
        this.allowFollowingTokens = allowFollowingTokens;
        this.ignoreBracketLogic = ignoreBracketLogic;
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

    public static MatcherConfig getLooseConfig() {
        return new MatcherConfig(true, true);
    }

    public static MatcherConfig getStrictConfig() {
        return new MatcherConfig(false, false);
    }
}
