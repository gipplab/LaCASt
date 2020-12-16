package gov.nist.drmf.interpreter.pom.extensions;

import gov.nist.drmf.interpreter.pom.MLPWrapper;
import gov.nist.drmf.interpreter.pom.SemanticMLPWrapper;
import org.intellij.lang.annotations.Language;

/**
 * @author Andre Greiner-Petter
 */
public class MatchablePomTaggedExpressionConfig {

    private final MLPWrapper mlpWrapper;

    private final GroupCaptures captures;

    @Language("RegExp")
    private String wildcardPattern;

    private boolean fallbackConsecutiveWildcards = true;

    public MatchablePomTaggedExpressionConfig(){
        this( SemanticMLPWrapper.getStandardInstance() );
    }

    public MatchablePomTaggedExpressionConfig(MLPWrapper mlpWrapper) {
        this( mlpWrapper, "" );
    }

    public MatchablePomTaggedExpressionConfig(MLPWrapper mlpWrapper, @Language("RegExp") String wildcardPattern) {
        this.mlpWrapper = mlpWrapper;
        this.wildcardPattern = wildcardPattern;
        this.captures = new GroupCaptures();
    }

    public MLPWrapper getMlpWrapper() {
        return mlpWrapper;
    }

    public GroupCaptures getCaptures() {
        return captures;
    }

    public String getWildcardPattern() {
        return wildcardPattern;
    }

    public MatchablePomTaggedExpressionConfig setWildcardPattern(@Language("RegExp") String wildcardPattern) {
        this.wildcardPattern = wildcardPattern;
        return this;
    }

    public boolean fallbackConsecutiveWildcards() {
        return fallbackConsecutiveWildcards;
    }

    public MatchablePomTaggedExpressionConfig setFallbackConsecutiveWildcards(boolean fallbackConsecutiveWildcards) {
        this.fallbackConsecutiveWildcards = fallbackConsecutiveWildcards;
        return this;
    }
}
