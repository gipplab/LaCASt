package gov.nist.drmf.interpreter.common.constants;

import java.util.regex.Pattern;

/**
 * Created by AndreG-P on 28.02.2017.
 */
public class GlobalConstants {
    public static final String NL = System.lineSeparator();

    public static final String POSITION_MARKER = "$";

    public static final String CARET_CHAR = "^";

    public static final String WHITESPACE = " ";

    private static final String LATEX_MULTIPLY = "\\\\cdot|\\\\idot|\\s*\\*\\s*";
    public static final Pattern LATEX_MULTIPLY_PATTERN = Pattern.compile(LATEX_MULTIPLY);

    private static final String LATEX_COMMAND = "\\\\[a-zA-Z()\\[\\]{}]+";
    public static final Pattern LATEX_COMMAND_PATTERN = Pattern.compile(LATEX_COMMAND);

    private static final String macro_pattern =
            "(X\\d:\\\\\\w+X)*(\\\\\\w+)(\\[[^@]+])*(\\{[^@]+})*(@+)*(\\{[^@]+})*\\s*";
    public static final Pattern DLMF_MACRO_PATTERN =
            Pattern.compile(macro_pattern);

    private static final String general_trans_macro_pattern =
            "(X\\d:\\\\\\w+X)*(\\\\\\w+)?.+";
    public static final Pattern GENERAL_MACRO_TRANSLATION_PATTERN =
            Pattern.compile(general_trans_macro_pattern);

    private static final String general_cas_func_pattern =
            "(X\\d:\\w+X)*(\\w+)[^\\w]?.*";
    public static final Pattern GENERAL_CAS_FUNC_PATTERN =
            Pattern.compile(general_cas_func_pattern);

    public static final int GEN_CAS_FUNC_SPECIFIER = 1;
    public static final int GEN_CAS_FUNC_PATTERN_NAME = 2;

    public static final String MACRO_OPT_PARAS_SPLITTER = ":";
    public static final int MACRO_PATTERN_INDEX_OPT_PARAS = 1;
    public static final int MACRO_PATTERN_INDEX_MACRO = 2;
    public static final int MACRO_PATTERN_INDEX_ATS = 5;
    public static final int MACRO_PATTERN_INDEX_OPT_PARAS_ELEMENTS = 3;

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
}
