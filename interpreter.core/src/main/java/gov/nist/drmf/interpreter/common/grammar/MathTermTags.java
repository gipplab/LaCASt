package gov.nist.drmf.interpreter.common.grammar;

import java.util.HashMap;

/**
 * @author Andre Greiner-Petter
 */
public enum MathTermTags {
    dlmf_macro("dlmf-macro"),
    symbol("symbol"),
    constant("mathematical constant"),
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
    left_delimiter("left-delimiter"),
    right_delimiter("right-delimiter"),
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
    greater_than("greater-than"),
    macro("macro"),
    caret("caret"),
    underscore("underscore"),
    ordinary("ordinary"),
    factorial("exclamation point"),
    operation("operation"),
    ellipsis("ellipsis"),
    abbreviation("abbreviation"),
    spaces("controlled space"),
    non_allowed("non-allowed escaped symbol"),
    relation("relation"),
    fence("fence"),
    special_math_letter("special math letter"),
    prime("single quote");

    private String tag;

    private static class HOLDER{
        static HashMap<String, MathTermTags> keymap = new HashMap<>();
    }

    MathTermTags (String tag){
        this.tag = tag;
        HOLDER.keymap.put(tag, this);
    }

    public static MathTermTags getTagByKey(String key){
        return HOLDER.keymap.get(key);
    }

    public String tag(){
        return tag;
    }
}
