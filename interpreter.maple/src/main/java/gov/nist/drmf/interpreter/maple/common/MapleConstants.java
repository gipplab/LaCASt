package gov.nist.drmf.interpreter.maple.common;

import java.util.regex.Pattern;

/**
 * This class contains all constants to translate maple expressions.
 * Created by AndreG-P on 27.02.2017.
 */
public final class MapleConstants {
    /**
     * A pattern to find + or - strings.
     */
    private static final String PLUS_MINUS_SIGN = "\\s*([+-])\\s*";
    public static final Pattern PLUS_MINUS_SIGN_PATTER = Pattern.compile(PLUS_MINUS_SIGN);

    /**
     * A pattern to find strings with negative sign in front
     */
    private static final String PREV_NEG_SIGN = "\\s*-\\s*([^\\s]+)";
    public static final Pattern PREV_NEG_SIGN_PATTERN = Pattern.compile( PREV_NEG_SIGN );

    /**
     * Constants for positive and negative values
     */
    public static final boolean POSITIVE = true;
    public static final boolean NEGATIVE = false;

    /**
     * String representations of positive and negative signs.
     */
    public static final String MINUS_SIGN = "-";
    public static final String PLUS_SIGN = "+";

    /**
     * All internal names should have this structure:
     *      _Inert_<Name>
     */
    private static final String MAPLE_INTERNAL_REGEX = "_Inert_([A-Z]+)";
    public static final Pattern MAPLE_INTERNAL_PATTERN = Pattern.compile( MAPLE_INTERNAL_REGEX );

    public static final String INFINITY = "infinity";
    public static final String I_UNIT = "I";

    public static final String[] LIST_OF_EXCLUDES = new String[]{"_Inert_FLOAT"};
}
