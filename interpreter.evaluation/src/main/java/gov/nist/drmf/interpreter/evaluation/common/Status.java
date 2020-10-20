package gov.nist.drmf.interpreter.evaluation.common;

import java.util.Arrays;

/**
 * The basic idea of the status is as follows
 *  1) total test cases = started test cases + skipped test cases + non-semantic definitions
 *      1.1) total are all test cases
 *      1.2) started test cases are all actually started (start to translated) cases
 *      1.3) skipped are user defined skips or unable to analyze test cases (e.g. no equation), etc.
 *      1.4) skipped defs are definitions of non-semantic expressions
 *  2) successful trans = successful test case + failure
 *
 * @author Andre Greiner-Petter
 */
public enum Status {
    TOTAL(0), // = skipped + definitions + started
    SKIPPED(0),
    DEFINITIONS(0),
    STARTED_TEST_CASES(0), // = error trans + missing + succes trans
    ERROR_TRANS(0),
    MISSING(0),
    SUCCESS_TRANS(0), // = success symb/num + failure + aborted + error
    SUCCESS_SYMB(0),
    SUCCESS_NUM(0),
    SUCCESS_UNDER_EXTRA_CONDITION(0),
    FAILURE(0),
    NO_TEST_VALUES(0),
    ABORTED(0),
    ERROR(0);

    private int counter;

    Status( int counter ){
        this.counter = counter;
    }

    public static void reset(){
        for ( Status s : Status.values() )
            s.counter = 0;
    }

    public void add(){
        switch (this) {
            case SKIPPED:
            case DEFINITIONS:
            case STARTED_TEST_CASES:
                TOTAL.counter++;
        }
        this.counter++;
    }

    public void set(int counter){
        this.counter = counter;
    }

    @Override
    public String toString(){
        return this.name() + ": " + counter;
    }

    public static String buildString(){
        return Arrays.toString(Status.values());
    }

    public static String buildNumericalString() {
        String out = "[TOTAL: " + TOTAL.counter + ", " +
                "SUCCESS: " +
                (SUCCESS_SYMB.counter > 0 ? SUCCESS_SYMB.counter : SUCCESS_NUM.counter)
                + ", ";
        out += "FAILURE: " + FAILURE.counter + ", ";
        out += "LIMIT_SKIPS: " + MISSING.counter + ", ";
        out += "TESTED: " + SUCCESS_TRANS.counter + ", ";
        out += "ERROR: " + ERROR.counter + "]";
        return out;
    }
}
