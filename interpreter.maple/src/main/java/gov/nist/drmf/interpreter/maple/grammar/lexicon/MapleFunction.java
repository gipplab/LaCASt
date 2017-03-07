package gov.nist.drmf.interpreter.maple.grammar.lexicon;

import gov.nist.drmf.interpreter.common.GlobalConstants;

/**
 *
 * Created by AndreG-P on 01.03.2017.
 */
public class MapleFunction {
    final String key;

    private final String MAPLE_Name;
    private final String MAPLE_Link;
    private final String DLMF_Pattern;
    private final String DLMF_Link;
    private final int numberOfVariables;

    private String maple_comment;
    private String domains;
    private String maple_branch_cuts;
    private String dlmf_branch_cuts;

    private String[] alternative_patterns;

    public MapleFunction(
            String MAPLE_Name,
            String DLMF_Pattern,
            String MAPLE_Link,
            String DLMF_Link,
            int numberOfVariables
    ){
        this.DLMF_Pattern = DLMF_Pattern;
        this.DLMF_Link = DLMF_Link;
        this.MAPLE_Name = MAPLE_Name;
        this.MAPLE_Link = MAPLE_Link;
        this.numberOfVariables = numberOfVariables;
        this.key = MapleLexicon.buildKey( MAPLE_Name, numberOfVariables );
    }

    public String replacePlaceHolders( String[] arguments ){
        String copy = DLMF_Pattern;
        for ( int i = 0; i < arguments.length; i++ ){
            copy = copy.replace(
                    GlobalConstants.POSITION_MARKER + Integer.toString(i),
                    arguments[i] );
        }
        return copy;
    }

    public void setMapleComment(String maple_comment) {
        this.maple_comment = maple_comment;
    }

    public void setDomains(String domains) {
        this.domains = domains;
    }

    public void setMapleBranchCuts(String maple_branch_cuts) {
        this.maple_branch_cuts = maple_branch_cuts;
    }

    public void setDlmfBranchCuts(String dlmf_branch_cuts) {
        this.dlmf_branch_cuts = dlmf_branch_cuts;
    }

    public void setAlternativePatterns(String... alternative_patterns) {
        this.alternative_patterns = alternative_patterns;
    }

    public String getMAPLEName() {
        return MAPLE_Name;
    }

    public String getMAPLELink() {
        return MAPLE_Link;
    }

    public String getDLMFPattern() {
        return DLMF_Pattern;
    }

    public String getDLMFLink() {
        return DLMF_Link;
    }

    public int getNumberOfVariables() {
        return numberOfVariables;
    }

    public String getMapleComment() {
        return maple_comment;
    }

    public String getDomains() {
        return domains;
    }

    public String getMapleBranchCuts() {
        return maple_branch_cuts;
    }

    public String getDlmfBranchCuts() {
        return dlmf_branch_cuts;
    }

    public String[] getAlternativePatterns() {
        return alternative_patterns;
    }
}
