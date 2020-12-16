package gov.nist.drmf.interpreter.pom.common.grammar;

import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.text.TextUtility;
import gov.nist.drmf.interpreter.pom.common.FeatureSetUtility;
import mlp.MathTerm;
import mlp.PomTaggedExpression;

import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Andre Greiner-Petter
 */
public enum FeatureValues {
    ACCENT(
            Keys.FEATURE_ACCENT,
            (s) -> {
                Pattern dotsPattern = Pattern.compile("(\\d+)-dotted");
                Matcher m = dotsPattern.matcher(s);
                if ( m.matches() ) {
                    return "d".repeat(Integer.parseInt(m.group(1))) + "ot";
                } else return s;
            }
    ),
    LATEX_FEATURE_KEY(
            FeatureSetUtility.LATEX_FEATURE_KEY,
            s -> s
    )
    ;

    private final String featureSetName;
    private final Function<String, String> valueMapper;

    FeatureValues(String featureSetName, Function<String, String> mapper) {
        this.featureSetName = featureSetName;
        this.valueMapper = mapper;
    }

    private List<String> map(String value) {
        List<String> values = TextUtility.splitAndNormalizeCommands(value);
        return values.stream().map( valueMapper ).collect(Collectors.toList());
    }

    public List<String> getFeatureValues(MathTerm mathTerm) {
        String value = mathTerm.getFeatureValue(featureSetName);
        return map(value);
    }

    public List<String> getFeatureValues(PomTaggedExpression expr) {
        String value = expr.getFeatureValue(featureSetName);
        return map(value);
    }
}
