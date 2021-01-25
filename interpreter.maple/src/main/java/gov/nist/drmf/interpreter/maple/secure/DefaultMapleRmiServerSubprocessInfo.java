package gov.nist.drmf.interpreter.maple.secure;

import gov.nist.drmf.interpreter.common.process.RmiSubprocessInfo;

import java.util.List;

/**
 * @author Andre Greiner-Petter
 */
public class DefaultMapleRmiServerSubprocessInfo implements RmiSubprocessInfo {
    @Override
    public String getClassName() {
        return MapleRmiServer.class.getName();
    }

    @Override
    public List<String> getJvmArgs() {
        return List.of(
                "-XX:HeapDumpPath=/dev/null",
                "-Xms2g",
                "-Xmx10g",
                "-Xss200M"
        );
    }
}
