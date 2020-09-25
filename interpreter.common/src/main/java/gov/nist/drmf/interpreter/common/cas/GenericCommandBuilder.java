package gov.nist.drmf.interpreter.common.cas;

import java.util.Collection;

/**
 * @author Andre Greiner-Petter
 */
public class GenericCommandBuilder {
    public static String makeListWithDelimiter(Collection<String> els) {
        return String.join(", ", els);
    }
}
