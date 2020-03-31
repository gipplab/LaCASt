package gov.nist.drmf.interpreter.generic.macro;

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
            "\\\\m(?:left|right)"
    );

    public static final String VAR_PREFIX = "var";
    public static final String PAR_PREFIX = "par";
    public static final String OPTIONAL_PAR_PREFIX = "opPar";

    private MacroHelper(){};

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

    public static int addIdx( int repeat, int counter, Character[] symbs, StringBuilder sb ) {
        for ( int i = 0; i < repeat; i++ ) {
            sb.append(symbs[0]).append("$").append(counter).append(symbs[1]);
            counter++;
        }
        return counter;
    }

    public static void fillListWithArguments(List<String> list, String argumentsList) {
        argumentsList = argumentsList.substring(1, argumentsList.length()-1);
        String[] elements = argumentsList.split("]\\[");
        for ( String e : elements ) {
            e = e.replaceAll("#", VAR_PREFIX);
            e = cleanString(e);
            list.add(e);
        }
    }

    public static void fillInnerList(String para, List<String> fill, List<String> ref) {
        if ( ref.isEmpty() ) {
            fill.add(para);
        } else {
            for ( String args : ref ) {
                fill.add(para + " " + args);
            }
        }
    }
}
