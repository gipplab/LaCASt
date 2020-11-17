package gov.nist.drmf.interpreter.pom.common.grammar;

import mlp.PomTaggedExpression;

import java.util.HashMap;

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
    equation("equation"),
    equation_array("equation array"),
    multi_case("multicase"),
    multi_case_single_case("case"),
    balanced_expression("balanced-expression"),
    accented("accented"),
    choose("choose");

    private final String tag;

    ExpressionTags(String tag) {
        this.tag = tag;
        HOLDER.keymap.put(tag, this);
    }

    public static ExpressionTags getTagByKey(String key) {
        return HOLDER.keymap.get(key);
    }

    public static ExpressionTags getTag(PomTaggedExpression pte) {
        return getTagByKey(pte.getTag());
    }

    public String tag() {
        return tag;
    }

    private static class HOLDER {
        static HashMap<String, ExpressionTags> keymap = new HashMap<>();
    }
}
