package gov.nist.drmf.interpreter.common.text;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Andre Greiner-Petter
 */
public final class TextUtility {

    private TextUtility() {}

    public static String appendPattern(String in, Pattern pattern, int group) {
        StringBuilder sb = new StringBuilder();
        Matcher m = pattern.matcher(in);
        while ( m.find() ) {
            m.appendReplacement(sb, m.group(group));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    public static List<String> splitAndNormalizeCommands(String in) {
        if ( in == null ) return new LinkedList<>();

        String[] elements = in.split(",");
        List<String> set = new LinkedList<>();
        for ( int i = 0; i < elements.length; i++ ) {
            elements[i] = elements[i].trim();
            elements[i] = elements[i].startsWith("\\") ? elements[i].substring(1) : elements[i];
            set.add(elements[i]);
        }
        return set;
    }

    public static <V> String join(String joiner, Iterable<V> iter, Function<V, String> elementMapper) {
        return join(joiner, iter, elementMapper, -1, "");
    }

    public static <V> String join(String joiner, Iterable<V> iter, Function<V, String> elementMapper, int max, String maxMessage) {
        StringBuilder sb = new StringBuilder();
        Iterator<V> iterator = iter.iterator();

        if (!iterator.hasNext()) return "";

        int number = 1;
        V last = iterator.next();
        sb.append(elementMapper.apply(last));
        while (iterator.hasNext()) {
            V next = iterator.next();
            if ( number >= max && max > 0 ) {
                sb.append( joiner ).append( maxMessage );
                break;
            } else sb.append( joiner ).append( elementMapper.apply(next) );
            number++;
        }
        return sb.toString();
    }
}
