package gov.nist.drmf.interpreter.common.grammar;

import mlp.MathTerm;

/**
 * @author Andre Greiner-Petter
 */
public enum MathTermTags {
    command("latex-command"),
    function("function"),
    letter("letter"),
    digit("digit"),
    numeric("numeric"),
    minus("minus"),
    plus("plus"),
    equals("equals"),
    multiply("star"),
    divide("forward slash"),
    left_parenthesis("left parenthesis"),
    right_parenthesis("right parenthesis"),
    left_bracket("left-bracket"),
    right_bracket("right-bracket"),
    left_brace("left-brace"),
    right_brace("right-brace"),
    at("at"),
    alphanumeric("alphanumeric"),
    comma("comma"),
    less_than("less-than"),
    greater_than("greater-then"),
    mod("mod"),
    macro("macro");

    private String tag;

    MathTermTags (String tag){
        this.tag = tag;
    }

    public static MathTermTags getTagByKey(String key){
        for (MathTermTags t : MathTermTags.values())
            if ( t.tag.matches(key) )
                return t;
        return null;
    }
}
