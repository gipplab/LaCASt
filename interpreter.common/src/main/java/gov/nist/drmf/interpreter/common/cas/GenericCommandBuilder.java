package gov.nist.drmf.interpreter.common.cas;

/**
 * @author Andre Greiner-Petter
 */
public class GenericCommandBuilder {
    public static String makeListWithDelimiter(java.util.List<String> els) {
        StringBuilder sb = new StringBuilder();
        sb.append(els.get(0));
        for ( int i = 1; i < els.size(); i++ ) {
            sb.append(", ").append(els.get(i));
        }
        return sb.toString();
    }
}
