package gov.nist.drmf.interpreter.common.grammar;

/**
 * Created by Andre Greiner-Petter on 02.11.2016.
 */
public enum ExpressionTags {
    sequence("sequence"),
    fraction("fraction"),
    binomial("binomial coefficient"),
    square_root("square root"),
    general_root("radical with a specified index"),
    sub_super_script("subsuperscript"),
    numerator("numerator"),
    denominator("denominator"),
    equation("equation");

    public String tag;

    ExpressionTags(String tag){
        this.tag = tag;
    }

    public static ExpressionTags getTagByKey(String key){
        for ( ExpressionTags tag : ExpressionTags.values() )
            if ( tag.tag.matches(key) ) return tag;
        return null;
    }

    public String tag(){
        return tag;
    }
}
