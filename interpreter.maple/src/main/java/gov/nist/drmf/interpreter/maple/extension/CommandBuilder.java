package gov.nist.drmf.interpreter.maple.extension;

import gov.nist.drmf.interpreter.common.cas.GenericCommandBuilder;

/**
 * @author Andre Greiner-Petter
 */
public final class CommandBuilder {
    private CommandBuilder() {}

    public static String makeMapleSet(java.util.List<String> els) {
        String s = GenericCommandBuilder.makeListWithDelimiter(els);
        return "{"+s+"}";
    }

    public static String makeMapleList(java.util.List<String> els ) {
        String s = GenericCommandBuilder.makeListWithDelimiter(els);
        return "["+s+"]";
    }
}
