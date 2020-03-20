package gov.nist.drmf.interpreter.maple.extension;

/**
 * @author Andre Greiner-Petter
 */
public final class CommandBuilder {
    private CommandBuilder() {}

    public static String makeMapleSet(java.util.List<String> els) {
        String s = makeListWithDelimiter(els);
        return "{"+s+"}";
    }

    public static String makeMapleList(java.util.List<String> els ) {
        String s = makeListWithDelimiter(els);
        return "["+s+"]";
    }

    public static String makeListWithDelimiter(java.util.List<String> els) {
        StringBuilder sb = new StringBuilder();
        sb.append(els.get(0));
        for ( int i = 1; i < els.size(); i++ ) {
            sb.append(", ").append(els.get(i));
        }
        return sb.toString();
    }
}
