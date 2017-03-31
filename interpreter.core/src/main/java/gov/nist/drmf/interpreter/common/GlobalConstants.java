package gov.nist.drmf.interpreter.common;

import java.util.regex.Pattern;

/**
 * Created by AndreG-P on 28.02.2017.
 */
public class GlobalConstants {
    public static final String POSITION_MARKER = "$";

    public static final String ALTERNATIVE_SPLIT = "\\|\\|";

    public static final String CARET_CHAR = "^";

    public static final String WHITESPACE = " ";

    public static final String LATEX_MULTIPLY = "\\\\cdot|\\s\\*";
    public static final Pattern LATEX_MULTIPLY_PATTERN = Pattern.compile(LATEX_MULTIPLY);

    public static final String LATEX_COMMAND = "\\\\[a-zA-Z\\(\\)\\[\\]\\{}]+";
    public static final Pattern LATEX_COMMAND_PATTERN = Pattern.compile(LATEX_COMMAND);

    public static final Pattern DLMF_MACRO_PATTERN =
            Pattern.compile("\\s*(\\\\\\w+)(\\[.*\\])*(\\{.*\\})*(@+\\{+.+\\}+)*\\s*");

    public static final String LINK_PREFIX = "http://";
    public static final String LINK_S_PREFIX = "https://";

    public static final String PROPS_COMMENTS =
            " Enter the command \"kernelopts(bindir);\" in Maple and put the given path into maple_bin.\n" +
            "# Since this is a java program and \\ is an escape character, you have to write \\\\ for each \\.\n" +
            "# In Unix based systems, this is not necessary.\n" +
            "#\n" +
            "# For example, in windows it could looks like this:\n" +
            "# maple_bin=C:\\\\Program Files\\\\Maple 2017\\\\bin.X86_64_WINDOWS\n" +
            "#\n" +
            "# Or in linux it could looks like this:\n" +
            "# maple_bin=/home/maple2016/bin.X86_64_LINUX";

    /**
     * A flag to identify the CAS at runtime. It is used for the
     * translation from DLMF to a given CAS.
     */
    public static String CAS_KEY = "";

    /**
     * A flag to translate not the direct translation but the alternative translation.
     * It is used by the MacroParser (interpreter.cas).
     *
     * It is probably useless because the alternative translation is not fully implemented
     * yet.
     */
    public static boolean ALTERNATIVE_MODE = false;
}
