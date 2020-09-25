package gov.nist.drmf.interpreter.maple.extension;

import gov.nist.drmf.interpreter.common.cas.GenericCommandBuilder;

import java.util.Collection;

/**
 * @author Andre Greiner-Petter
 */
public final class CommandBuilder {
    private CommandBuilder() {}

    public static String makeMapleSet(Collection<String> els) {
        String s = GenericCommandBuilder.makeListWithDelimiter(els);
        return "{"+s+"}";
    }

    public static String makeMapleList(Collection<String> els ) {
        String s = GenericCommandBuilder.makeListWithDelimiter(els);
        return "["+s+"]";
    }
}
