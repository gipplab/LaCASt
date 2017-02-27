package gov.nist.drmf.interpreter.common.grammar;

import java.util.HashMap;

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
    ),
    left_latex_parenthesis(
            Brackets.LATEX_LEFT + OPEN_BRACKETS.left_parenthesis.s,
            Brackets.OPENED,
            Brackets.LATEX_RIGHT + OPEN_BRACKETS.left_parenthesis.counter
    ),
    right_latex_parenthesis(
            Brackets.LATEX_RIGHT + CLOSE_BRACKETS.right_parenthesis.s,
            Brackets.CLOSED,
            Brackets.LATEX_LEFT + CLOSE_BRACKETS.right_parenthesis.counter
    );

    private static final String LATEX_LEFT = "\\left";
    private static final String LATEX_RIGHT = "\\right";

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
    public static final String CLOSED_PATTERN = "[\\)\\]\\}\\>]";

    public final String symbol;
    public final boolean opened;
    public final String counterpart;

    private static class HOLDER {
        static HashMap<String, Brackets> key_map = new HashMap<>();
    }

    Brackets(String symbol, boolean opened, String counterpart){
        this.symbol = symbol;
        this.opened = opened;
        this.counterpart = counterpart;
        HOLDER.key_map.put( symbol, this );
    }

    public Brackets getCounterPart(){
        return HOLDER.key_map.get( counterpart );
    }

    public static Brackets getBracket(String bracket){
        return HOLDER.key_map.get( bracket );
    }
}
