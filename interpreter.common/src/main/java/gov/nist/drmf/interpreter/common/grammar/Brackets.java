package gov.nist.drmf.interpreter.common.grammar;

import java.util.HashMap;

/**
 * This enumeration provides all kind of brackets. It can be split
 * into 2 groups, open and closed brackets. Each bracket has a symbol
 * and it's counterpart.
 *
 * For instance the left parenthesis is an open bracket,
 * the symbol is ( and the counterpart symbol is ).
 *
 * @author Andre Greiner-Petter
 */
public enum Brackets {
    /**
     * Left Open Parenthesis: (
     */
    left_parenthesis(
            OPEN_BRACKETS.left_parenthesis.s,
            Brackets.OPENED,
            OPEN_BRACKETS.left_parenthesis.counter
    ),
    /**
     * Left Open Bracket: [
     */
    left_brackets(
            OPEN_BRACKETS.left_brackets.s,
            Brackets.OPENED,
            OPEN_BRACKETS.left_brackets.counter
    ),
    /**
     * Left Open Braces: {
     */
    left_braces(
            OPEN_BRACKETS.left_braces.s,
            Brackets.OPENED,
            OPEN_BRACKETS.left_braces.counter
    ),
    /**
     * Left Open Angle Brackets: <
     */
    left_angle_brackets(
            OPEN_BRACKETS.left_angle_brackets.s,
            Brackets.OPENED,
            OPEN_BRACKETS.left_angle_brackets.counter
    ),
    /**
     * Right Closed Parenthesis: )
     */
    right_parenthesis(
            CLOSE_BRACKETS.right_parenthesis.s,
            Brackets.CLOSED,
            CLOSE_BRACKETS.right_parenthesis.counter
    ),
    /**
     * Right Closed Bracket: ]
     */
    right_brackets(
            CLOSE_BRACKETS.right_brackets.s,
            Brackets.CLOSED,
            CLOSE_BRACKETS.right_brackets.counter
    ),
    /**
     * Right Closed Braces: }
     */
    right_braces(
            CLOSE_BRACKETS.right_braces.s,
            Brackets.CLOSED,
            CLOSE_BRACKETS.right_braces.counter
    ),
    /**
     * Right Closed Angle Brackets: >
     */
    right_angle_brackets(
            CLOSE_BRACKETS.right_angle_brackets.s,
            Brackets.CLOSED,
            CLOSE_BRACKETS.right_angle_brackets.counter
    ),
    /**
     * Left Open LaTeX Parenthesis: \left(
     */
    left_latex_parenthesis(
            Brackets.LATEX_LEFT + OPEN_BRACKETS.left_parenthesis.s,
            Brackets.OPENED,
            Brackets.LATEX_RIGHT + OPEN_BRACKETS.left_parenthesis.counter
    ),
    /**
     * Right Closed LaTeX Parenthesis: \right)
     */
    right_latex_parenthesis(
            Brackets.LATEX_RIGHT + CLOSE_BRACKETS.right_parenthesis.s,
            Brackets.CLOSED,
            Brackets.LATEX_LEFT + CLOSE_BRACKETS.right_parenthesis.counter
    ),
    /**
     * Left Open LaTeX Parenthesis: \left(
     */
    left_latex_brackets(
            Brackets.LATEX_LEFT + OPEN_BRACKETS.left_brackets.s,
            Brackets.OPENED,
            Brackets.LATEX_RIGHT + OPEN_BRACKETS.left_brackets.counter
    ),
    /**
     * Right Closed LaTeX Parenthesis: \right)
     */
    right_latex_brackets(
            Brackets.LATEX_RIGHT + CLOSE_BRACKETS.right_brackets.s,
            Brackets.CLOSED,
            Brackets.LATEX_LEFT + CLOSE_BRACKETS.right_brackets.counter
    ),
    /**
     * Left Opened LaTeX Pipe: \left|
     */
    left_latex_abs_val(
            Brackets.LATEX_LEFT + OPEN_BRACKETS.left_vbar.s,
            Brackets.OPENED,
            Brackets.LATEX_RIGHT + OPEN_BRACKETS.left_vbar.counter
    ),
    /**
     * Right Closed LaTeX Pipe: \right|
     */
    right_latex_abs_val(
            Brackets.LATEX_RIGHT + CLOSE_BRACKETS.right_vbar.s,
            Brackets.CLOSED,
            Brackets.LATEX_LEFT + CLOSE_BRACKETS.right_vbar.counter
    ),
    /**
     * Right Closed LaTeX Pipe: |
     */
    abs_val_close(
            OPEN_BRACKETS.left_vbar.s,
            Brackets.CLOSED,
            OPEN_BRACKETS.left_vbar.counter
    ),
    /**
     * Left Opened LaTeX Pipe: |
     */
    abs_val_open(
            OPEN_BRACKETS.left_vbar.s,
            Brackets.OPENED,
            OPEN_BRACKETS.left_vbar.counter
    );

    /**
     * Prefix for left and right parenthesis in latex
     */
    private static final String LATEX_LEFT = "\\left";
    private static final String LATEX_RIGHT = "\\right";

    /**
     * For initialization reasons, the open brackets
     * are an extra private enumeration.
     * The symbols and counter symbols are stored here.
     */
    private enum OPEN_BRACKETS{
        left_parenthesis("(", ")"),
        left_brackets("[", "]"),
        left_braces("{", "}"),
        left_angle_brackets("<", ">"),
        left_vbar("|", "|");

        final String s;
        final String counter;

        OPEN_BRACKETS(String s, String counter){
            this.s = s;
            this.counter = counter;
        }
    }

    /**
     * For initialization reasons, the closed brackets
     * are an extra private enumeration.
     * The symbols and counter symbols are stored here.
     */
    private enum CLOSE_BRACKETS{
        right_parenthesis(")", "("),
        right_brackets("]", "["),
        right_braces("}", "{"),
        right_angle_brackets(">", "<"),
        right_vbar("|", "|");

        final String s;
        final String counter;

        CLOSE_BRACKETS(String s, String counter){
            this.s = s;
            this.counter = counter;
        }
    }

    /**
     * Is the bracket opening or closing
     */
    public static final boolean OPENED = true;
    public static final boolean CLOSED = false;

    /**
     * Patterns for open brackets and closed brackets
     */
    public static final String OPEN_PATTERN = "[\\(\\[\\{|]";
    public static final String CLOSED_PATTERN = "[\\)\\]\\}|]";

    /**
     * Each bracket is open or closed and has a symbol and its counterpart symbol
     */
    public final String symbol;
    public final boolean opened;
    public final String counterpart;

    /**
     * Initialization on demand. To speed up the search of enumeration.
     */
    private static class HOLDER {
        static HashMap<String, Brackets> key_map = new HashMap<>();
    }

    /**
     * Bracket with symbol and counterpart symbol and if its closed or not.
     * @param symbol (, [, {, <, ), ], }, >, or |
     * @param opened true or false (opened or closed)
     * @param counterpart Depending on the symbol.
     *                    ), ], }, >, (, [, {, <, or |
     */
    Brackets(String symbol, boolean opened, String counterpart){
        this.symbol = symbol;
        this.opened = opened;
        this.counterpart = counterpart;
        HOLDER.key_map.put( symbol, this );
    }

    public String getAppropriateString(){
        if ( symbol.matches("\\\\(left|right).*") )
            return symbol.substring( symbol.length()-1 );
        else return symbol;
    }

    /**
     * Returns the counter part of a bracket.
     * @return Bracket object of the counter part. For instance the counterpart
     *          of left_parenthesis is right_parenthesis.
     */
    public Brackets getCounterPart(){
        return HOLDER.key_map.get( counterpart );
    }

    /**
     * Returns the bracket corresponding to the given string representation of a bracket
     * @param bracket string of (, [, {, <, ), ], } or >
     * @return the enum object
     */
    public static Brackets getBracket(String bracket){
        bracket = bracket.replaceAll("\\s", "");
        return HOLDER.key_map.get( bracket );
    }
}