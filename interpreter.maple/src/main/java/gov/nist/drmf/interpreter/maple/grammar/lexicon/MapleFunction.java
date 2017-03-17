package gov.nist.drmf.interpreter.maple.grammar.lexicon;

import gov.nist.drmf.interpreter.common.GlobalConstants;

import java.util.Arrays;

/**
 *
 * Created by AndreG-P on 01.03.2017.
 */
public class MapleFunction {
    public static final String INNER_DELIMITER = ":=";

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

    private String[] toStringArray(){
        String alts = "";
        for ( int i = 0; i < alternative_patterns.length; i++ ){
            alts += alternative_patterns[i];
            if ( i < alternative_patterns.length-1 )
                alts += GlobalConstants.ALTERNATIVE_SPLIT;
        }

        return new String[]{
            key,
            "Maple-Name"+ INNER_DELIMITER + " " + MAPLE_Name,
            "DLMF-Pattern"+ INNER_DELIMITER + " " + DLMF_Pattern,
            "Maple-Link"+ INNER_DELIMITER + " " + MAPLE_Link,
            "NumberOfVars"+ INNER_DELIMITER + " " + numberOfVariables,

            "MapleComment"+ INNER_DELIMITER + " " + maple_comment,
            "MapleConstraint"+ INNER_DELIMITER + " " + maple_constraints,
            "MapleBranchCuts"+ INNER_DELIMITER + " " + maple_branch_cuts,
            "Alternatives"+ INNER_DELIMITER + " " + alts,
            "DLMF-Meaning"+ INNER_DELIMITER + " " + dlmf_meaning,
            "DLMF-Constraints"+ INNER_DELIMITER + " " + dlmf_constraints,
            "DLMF-BranchCuts"+ INNER_DELIMITER + " " + dlmf_branch_cuts,
            "DLMF-Link"+ INNER_DELIMITER + " " + dlmf_Link
        };
    }

    @Override
    public String toString(){
        String nl = System.lineSeparator();
        String str = "";
        if ( dlmf_meaning != null && !dlmf_meaning.isEmpty() )
            str += dlmf_meaning + nl;
        else str += nl;

        if ( maple_comment != null && !maple_comment.isEmpty() )
            str += "\tComment: " + maple_comment + nl;
        if ( dlmf_constraints != null && !dlmf_constraints.isEmpty() )
            str += "\tDLMF-Constraints: " + dlmf_constraints + nl;
        if ( dlmf_branch_cuts != null && !dlmf_branch_cuts.isEmpty() )
            str += "\tDLMF-Branch Cuts: " + dlmf_branch_cuts + nl;

        str += "\tMaple-Link: " + MAPLE_Link + nl;
        str += "\tDLMF-Link:  " + dlmf_Link;
        return str;
    }

    public static String toStorage( MapleFunction mf ){
        String nl = System.lineSeparator();
        String[] a = mf.toStringArray();
        String out = a[0] + nl;
        for ( int i = 1; i < a.length; i++ )
            out += "\t" + a[i] + nl;
        return out+nl;
    }

    public static MapleFunction loadMapleFunction( String infos ){
        try {
            String[] infs = infos.split( System.lineSeparator() );
            MapleFunction mf = new MapleFunction(
                    infs[1].split(INNER_DELIMITER)[1].trim(),
                    infs[2].split(INNER_DELIMITER)[1].trim(),
                    infs[3].split(INNER_DELIMITER)[1].trim(),
                    Integer.parseInt(infs[4].split(INNER_DELIMITER)[1].trim())
            );

            mf.maple_comment        = infs[5].split(INNER_DELIMITER)[1].trim();
            mf.maple_constraints    = infs[6].split(INNER_DELIMITER)[1].trim();
            mf.maple_branch_cuts    = infs[7].split(INNER_DELIMITER)[1].trim();
            mf.alternative_patterns =
                    infs[8].split(INNER_DELIMITER)[1].trim()
                            .split( GlobalConstants.ALTERNATIVE_SPLIT );

            mf.dlmf_meaning     = infs[9].split(INNER_DELIMITER)[1].trim();
            mf.dlmf_constraints = infs[10].split(INNER_DELIMITER)[1].trim();
            mf.dlmf_branch_cuts = infs[11].split(INNER_DELIMITER)[1].trim();
            mf.dlmf_Link        = infs[12].split(INNER_DELIMITER)[1].trim();
            return mf;
        } catch ( Exception e ){
            System.err.println(infos);
            e.printStackTrace();
            return null;
        }
    }
}
