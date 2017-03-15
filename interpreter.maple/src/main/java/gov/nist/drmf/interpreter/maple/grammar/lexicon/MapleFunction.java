package gov.nist.drmf.interpreter.maple.grammar.lexicon;

import gov.nist.drmf.interpreter.common.GlobalConstants;

import java.util.Arrays;

/**
 *
 * Created by AndreG-P on 01.03.2017.
 */
public class MapleFunction {
    final String key;

    private final String MAPLE_Name;
    private final String MAPLE_Link;
    private final String DLMF_Pattern;
    private final int numberOfVariables;

    private String maple_comment;
    private String maple_branch_cuts;
    private String maple_constraints;

    private String dlmf_branch_cuts;
    private String dlmf_constraints;
    private String dlmf_meaning;
    private String dlmf_Link;

    private String[] alternative_patterns;

    public MapleFunction(
            String MAPLE_Name,
            String DLMF_Pattern,
            String MAPLE_Link,
            int numberOfVariables
    ){
        this.DLMF_Pattern = DLMF_Pattern;
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

    public void setMapleConstraints(String maple_constraints) {
        this.maple_constraints = maple_constraints;
    }

    public void setMapleBranchCuts(String maple_branch_cuts) {
        this.maple_branch_cuts = maple_branch_cuts;
    }

    public void setDlmfLink( String dlmf_link ){
        this.dlmf_Link = dlmf_link;
    }

    public void setDlmfBranchCuts(String dlmf_branch_cuts) {
        this.dlmf_branch_cuts = dlmf_branch_cuts;
    }

    public void setAlternativePatterns(String... alternative_patterns) {
        this.alternative_patterns = alternative_patterns;
    }

    public void setDlmfConstraints(String dlmf_constraints) {
        this.dlmf_constraints = dlmf_constraints;
    }

    public void setDlmfMeaning(String dlmf_meaning) {
        this.dlmf_meaning = dlmf_meaning;
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
        return dlmf_Link;
    }

    public int getNumberOfVariables() {
        return numberOfVariables;
    }

    public String getMapleComment() {
        return maple_comment;
    }

    public String getMapleConstraints() {
        return maple_constraints;
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

    public String getDlmfConstraints() {
        return dlmf_constraints;
    }

    public String getDlmfMeaning() {
        return dlmf_meaning;
    }

    String[] toStringArray(){
        String a = alternative_patterns == null ?
                "" : Arrays.toString(alternative_patterns);
        return new String[]{
            "KEY: " + key,
            "Maple-Func:     " + MAPLE_Name,
            "DLMF-Pattern:   " + DLMF_Pattern,
            "DLMF-Meaning:   " + dlmf_meaning,
            "Alternatives:   " + a,
            "Maple-Link:     " + MAPLE_Link,
            "DLMF-Link:      " + dlmf_Link,
            "NumberOfVars:   " + numberOfVariables,
            "MapleComment:   " + maple_comment,
            "MapleConstraint:" + maple_constraints,
            "MapleBranchCuts:" + maple_branch_cuts,
            "DLMFConstraints:" + dlmf_constraints,
            "DLMFBranchCuts: " + dlmf_branch_cuts
        };
    }

    @Override
    public String toString(){
        String nl = System.lineSeparator();
        String str = "";
        String[] output = toStringArray();
        for ( int i = 0; i < output.length; i++ )
            str += output[i] + nl;
        return str;
    }
}
