package gov.nist.drmf.interpreter.generic.macro;

import gov.nist.drmf.interpreter.common.latex.TeXPreProcessor;
import gov.nist.drmf.interpreter.generic.mlp.pojo.SemanticReplacementRule;
import gov.nist.drmf.interpreter.pom.extensions.MatcherConfig;
import gov.nist.drmf.interpreter.pom.moi.MOINode;
import org.intellij.lang.annotations.Language;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Andre Greiner-Petter
 */
public final class MacroHelper {
    public static final Pattern CLEAN_PATTERN = Pattern.compile(
            "\\\\m(?:left|right|iddle)"
    );

    public static final String VAR_PREFIX = "var";
    public static final String PAR_PREFIX = "par";
    public static final String OPTIONAL_PAR_PREFIX = "opPar";

    public static final Pattern NUM_PATTERN = Pattern.compile("#(\\d+)");

    @Language("RegExp")
    public static final String WILDCARD_PATTERNS = "(?:opP|p|v)ar\\d+";

    private MacroHelper(){}

    public static String cleanArgument(String arg, String pattern) {
        arg = cleanString(arg);
        arg = fixInvisibleComma(arg);
        arg = TeXPreProcessor.trimIfWrappedInCurlyBrackets(arg);
        arg = TeXPreProcessor.normalizeGenFrac(arg);
        return arg.replaceAll("#", pattern);
    }

    public static String cleanString(String arg) {
        StringBuilder sb = new StringBuilder();
        Matcher cleanMatcher = CLEAN_PATTERN.matcher(arg);
        while ( cleanMatcher.find() ) {
            cleanMatcher.appendReplacement(sb, "");
        }
        cleanMatcher.appendTail(sb);
        return sb.toString();
    }

    public static String generateArgumentList(int numberOfArgs) {
        if ( numberOfArgs == 1 ) return "(" + VAR_PREFIX+1 + ")";

        StringBuilder sb = new StringBuilder("(" + VAR_PREFIX + 1);
        for ( int i = 2; i <= numberOfArgs; i++ ) {
            sb.append(",").append(VAR_PREFIX).append(i);
        }
        sb.append(")");
        return sb.toString();
    }

    public static LinkedList<String> generateListOfArguments(String args) {
        if (args.length() < 1) {
            throw new IllegalArgumentException("Empty arguments list cannot be handled.");
        }

        if (args.matches("^\\{.*}$"))
            args = args.substring(1, args.length() - 1);

        String[] elements = args.split("}\\{");
        return new LinkedList<>(Arrays.asList(elements));
    }

    public static void addIdx( String prefix, int repeat, Character[] symbs, StringBuilder sb ) {
        for ( int i = 1; i <= repeat; i++ ) {
            sb.append(symbs[0]).append(prefix).append(i).append(symbs[1]);
        }
    }

    public static void fillListWithArguments(int numOfArgs, List<String> list, List<Boolean[]> defaultArgs, String argumentsList) {
        argumentsList = argumentsList.substring(1, argumentsList.length()-1);
        String[] elements = argumentsList.split("]\\[");
        for ( String e : elements ) {
            Boolean[] defaultArg = generateBooleanDefArg(numOfArgs, e);
            defaultArgs.add(defaultArg);

            e = e.replaceAll("#", VAR_PREFIX);
            e = cleanString(e);
            list.add(e);
        }
    }

    public static Boolean[] generateBooleanDefArg(int numOfArgs, String str) {
        Boolean[] defaultArg = new Boolean[numOfArgs];
        Matcher m = NUM_PATTERN.matcher(str);
        while ( m.find() ) {
            defaultArg[Integer.parseInt(m.group(1))-1] = true;
        }
        return defaultArg;
    }

    public static Boolean[] allTrueArr(int numOfArgs) {
        return fillArr(numOfArgs, true);
    }

    public static Boolean[] fillArr(int numOfArgs, boolean value) {
        Boolean[] defaultArg = new Boolean[numOfArgs];
        Arrays.fill(defaultArg, value);
        return defaultArg;
    }

    public static void fillInnerList(String para, List<String> fill, List<String> ref) {
        fill.add(para);
        for ( String args : ref ) {
            fill.add(para + " " + args);
        }
    }

    public static double calculateScore(int var, Boolean[] defaults) {
        int num = 0;
        for ( Boolean b : defaults )
            if ( b != null && b ) num++;
        return (num)/(double)var;
    }

    public static void generateDefaultArgList(StringBuilder innerSB, Boolean[] defArgs, List<String> defaultArgs) {
        for ( int k = 1; k <= defArgs.length; k++ ) {
            Boolean useDef = defArgs[k-1];
            innerSB.append("{");
            if ( useDef != null && useDef ) {
                innerSB.append(MacroHelper.VAR_PREFIX).append(k);
            } else {
                String def = defaultArgs.get(k-1);
                innerSB.append(def);
            }
            innerSB.append("}");
        }
    }

    public static String fixInvisibleComma(String in) {
        return in.replaceAll("\\\\InvisibleComma", " ");
    }

    public static MatcherConfig getMatchingConfig(SemanticReplacementRule replacementRule, MOINode<?> node) {
        MacroBean bean = replacementRule.getMacro();
        MatcherConfig config = MatcherConfig.getInPlaceMatchConfig();
        if ( node.isSource() && !node.isSink() ) config = MatcherConfig.getExactMatchConfig();
        config = updateMatchingConfig(bean, config);
        String genericTex = replacementRule.getPattern().getGenericTex();
        if ( !genericTex.matches(".*(overline|tilde).*") ) config.ignoreFontManipulation(true);
        return config;
    }

    public static MatcherConfig updateMatchingConfig(MacroBean bean, MatcherConfig config) {
        MacroStandardArgumentsBean standardArgs = bean.getMetaInformation().getStandardArguments();
        setWildcardConfig( standardArgs.getStandardOptionalParameters(), OPTIONAL_PAR_PREFIX, config );
        setWildcardConfig( standardArgs.getStandardParameters(), PAR_PREFIX, config );
        setWildcardConfig( standardArgs.getStandardVariables(), VAR_PREFIX, config );
        return config;
    }

    private static void setWildcardConfig(LinkedList<String> vars, String prefix, MatcherConfig config) {
        for ( int i = 1; i <= vars.size(); i++ ) {
            if ( vars.get(i-1).contains(",") ) config.allowCommaForWildcard( prefix+i );
        }
    }
}
