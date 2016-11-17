package gov.nist.drmf.interpreter.common.grammar;

/**
 * @author Andre Greiner-Petter
 */
public enum Brackets {
    left_parenthesis(
            OPEN_BRACKETS.left_parenthesis.s,
            Brackets.OPENED,
            OPEN_BRACKETS.left_parenthesis.counter
    ),
    left_brackets(
            OPEN_BRACKETS.left_brackets.s,
            Brackets.OPENED,
            OPEN_BRACKETS.left_brackets.counter
    ),
    left_braces(
            OPEN_BRACKETS.left_braces.s,
            Brackets.OPENED,
            OPEN_BRACKETS.left_braces.counter
    ),
    left_angle_brackets(
            OPEN_BRACKETS.left_angle_brackets.s,
            Brackets.OPENED,
            OPEN_BRACKETS.left_angle_brackets.counter
    ),
    right_parenthesis(
            CLOSE_BRACKETS.right_parenthesis.s,
            Brackets.CLOSED,
            CLOSE_BRACKETS.right_parenthesis.counter
    ),
    right_brackets(
            CLOSE_BRACKETS.right_brackets.s,
            Brackets.CLOSED,
            CLOSE_BRACKETS.right_brackets.counter
    ),
    right_braces(
            CLOSE_BRACKETS.right_braces.s,
            Brackets.CLOSED,
            CLOSE_BRACKETS.right_braces.counter
    ),
    right_angle_brackets(
            CLOSE_BRACKETS.right_angle_brackets.s,
            Brackets.CLOSED,
            CLOSE_BRACKETS.right_angle_brackets.counter
    );

    private enum OPEN_BRACKETS{
        left_parenthesis("(", ")"),
        left_brackets("[", "]"),
        left_braces("{", "}"),
        left_angle_brackets("<", ">");

        final String s;
        final String counter;

        OPEN_BRACKETS(String s, String counter){
            this.s = s;
            this.counter = counter;
        }
    }

    private enum CLOSE_BRACKETS{
        right_parenthesis(")", "("),
        right_brackets("]", "["),
        right_braces("}", "{"),
        right_angle_brackets(">", "<");

        final String s;
        final String counter;

        CLOSE_BRACKETS(String s, String counter){
            this.s = s;
            this.counter = counter;
        }
    }

    public static final boolean OPENED = true;
    public static final boolean CLOSED = false;

    public static final String OPEN_PATTERN = "[\\(\\[\\{\\<]";

    public final String symbol;
    public final boolean opened;
    public final String counterpart;

    Brackets(String symbol, boolean opened, String counterpart){
        this.symbol = symbol;
        this.opened = opened;
        this.counterpart = counterpart;
    }

    public static Brackets getBracket(String bracket){
        for ( Brackets b : Brackets.values() )
            if ( b.symbol.equals(bracket) )
                return b;
        return null;
    }
}
