package gov.nist.drmf.interpreter.common.eval;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Andre Greiner-Petter
 */
public final class EvaluationSkipper {

    private EvaluationSkipper() {}

    public static final Pattern COMPUTATION_SKIP_PATTERN = Pattern.compile(
            // must not contain an _ except its because of sum/int/prod/lim
            "^(?!.*(?<!(sum|int|prod|lim)\\s{0,2}(\\^\\{.{1,100}?}|\\^.)?)_)" +
            // must contain at least one = or relation symbol unless the equal sign was attached to sum/prod
            "(?=.*((?<!(sum|prod)(\\^\\{.{1,100}?}|\\^.)?_\\{[^}]{1,10})=" +
                    "|<|>|=>|=<|<=|>=|\\\\[lgn]eq?[^a-zA-Z]))" +
            ".*$"
    );

    public static boolean shouldBeEvaluated(String latex) {
        Matcher m = COMPUTATION_SKIP_PATTERN.matcher(latex);
        return m.matches();
    }

    public static boolean shouldNotBeEvaluated(String latex) {
        return !shouldBeEvaluated(latex);
    }
}
