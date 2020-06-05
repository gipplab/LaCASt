package gov.nist.drmf.interpreter.common.interfaces;

import java.util.Set;

/**
 * @author Andre Greiner-Petter
 */
public interface IPackageWrapper<IN, OUT> {
    OUT addPackages( IN expression, Set<String> packages );
}
