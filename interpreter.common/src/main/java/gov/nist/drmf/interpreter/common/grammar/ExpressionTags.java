package gov.nist.drmf.interpreter.common.grammar;

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
    balanced_expression("balanced-expression"),
    accented("accented");

    private final String tag;

    ExpressionTags(String tag) {
        this.tag = tag;
        HOLDER.keymap.put(tag, this);
    }

    public static ExpressionTags getTagByKey(String key) {
        return HOLDER.keymap.get(key);
    }

    public String tag() {
        return tag;
    }

    private static class HOLDER {
        static HashMap<String, ExpressionTags> keymap = new HashMap<>();
    }
}
