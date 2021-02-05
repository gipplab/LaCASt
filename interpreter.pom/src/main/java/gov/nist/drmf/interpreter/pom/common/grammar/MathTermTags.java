package gov.nist.drmf.interpreter.pom.common.grammar;

import mlp.MathTerm;
import mlp.PomTaggedExpression;

import java.util.HashMap;

/**
 * @author Andre Greiner-Petter
 */
public enum MathTermTags {
    dlmf_macro("dlmf-macro"),
    symbol("symbol"),
    constant("mathematical constant"),
    command("latex-command"),
    colon("colon"),
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
    point("point"),
    semicolon("semicolon"),
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
    prime("single quote"),
    primes("primes"),
    operator("operator"),
    modulo("modulo"),
    vbar("vertical-bar"),
    probability_dist("probability distribution"),
    newline("newline"),
    negated_equals("negated equals");

    public static final String OPEN_PARENTHESIS_PATTERN =
            "(left)[-\\s](parenthesis|bracket|brace|delimiter)|vertical-bar";

    public static final String CLOSE_PARENTHESIS_PATTERN =
            "(right)[-\\s](parenthesis|bracket|brace|delimiter)|vertical-bar";

    public static final String PARENTHESIS_PATTERN =
            "(right|left)[-\\s](parenthesis|bracket|brace|delimiter)|vertical-bar";

    private static final class HOLDER{
        static HashMap<String, MathTermTags> keymap = new HashMap<>();
    }

    private String tag;

    MathTermTags (String tag){
        this.tag = tag;
        HOLDER.keymap.put(tag, this);
    }

    public static MathTermTags getTagByKey(String key){
        return HOLDER.keymap.get(key);
    }

    public static MathTermTags getTagByMathTerm(MathTerm term) {
        if ( term == null ) return null;
        String termTag = term.getTag();
        return MathTermTags.getTagByKey(termTag);
    }

    public static MathTermTags getTagByExpression(PomTaggedExpression exp) {
        if ( exp == null ) return null;
        MathTerm term = exp.getRoot();
        return getTagByMathTerm(term);
    }

    public static boolean is(MathTerm term, MathTermTags tag) {
        return tag.equals(MathTermTags.getTagByMathTerm(term));
    }

    public static boolean is(PomTaggedExpression pte, MathTermTags tag) {
        return tag.equals(MathTermTags.getTagByExpression(pte));
    }

    public String tag(){
        return tag;
    }

    public static boolean isChooseCommand(MathTerm mt) {
        if ( mt == null || mt.isEmpty() ) return false;
        if ( MathTermTags.command.equals(getTagByKey(mt.getTag())) ) {
            return "\\choose".equals(mt.getTermText());
        }
        return false;
    }
}
