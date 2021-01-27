package gov.nist.drmf.interpreter.common.process;

import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Andre Greiner-Petter
 */
public interface RmiSubprocessInfo {
    /**
     * Returns the name of the class (including package path). You can usually
     * get thy by {@link Class#getName()}. For example:
     *
     * <code>
     *      RmiSubprocessInfo.class.getName();
     * </code>
     *
     * @return the name of the class to start including package path
     */
    String getClassName();

    /**
     * Returns an extra number of JVM args (like -Xmx or -Xss)
     * @return list of JVM args that should be used
     */
    List<String> getJvmArgs();

    /**
     * Builds the list of arguments
     * @return the list of arguments to start subprocess
     */
    default List<String> getCommandLineArguments() {
        String javaHome = System.getProperty(ProcessKeys.JAVA_HOME);
        String javaBin = Paths.get(javaHome, "bin", "java").toString();
        String classpath = System.getProperty(ProcessKeys.JAVA_CLASSPATH);
        String className = getClassName();

        List<String> command = new LinkedList<>();
        command.add(javaBin);
        command.addAll(getJvmArgs());
        command.add(ProcessKeys.JAVA_CLASSPATH_FLAG);
        command.add(classpath);
        command.add(className);

        return command;
    }
}