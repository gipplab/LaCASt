package gov.nist.drmf.interpreter.common.process;

import gov.nist.drmf.interpreter.common.constants.Keys;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * This starts another class in a different VM! It shares most of the same settings as the currently running
 * VM. The goal is to generate a sub-process in a different VM that can automatically recover from fatal errors, e.g.,
 * from SIGSEGV in native code.
 *
 * @author Andre Greiner-Petter
 */
public class RmiProcessHandler {
    private static final Logger LOG = LogManager.getLogger(RmiProcessHandler.class.getName());

    public static final String READY_SIGNAL = "SUCCESSFULLY SETUP SUBPROCESS READY TO WORK";

    private final ProcessBuilder processBuilder;

    private Process process = null;

    private SubprocessLoggerRunner infoLogRunner;
    private SubprocessLoggerRunner errorLogRunner;

    public RmiProcessHandler(Class<?> clazz) {
        // setup commands
        String javaHome = System.getProperty(ProcessKeys.JAVA_HOME);
        String javaBin = Paths.get(javaHome, "bin", "java").toString();
        String classpath = System.getProperty(ProcessKeys.JAVA_CLASSPATH);
        String className = clazz.getName();

        List<String> command = new LinkedList<>();
        command.add(javaBin);
        command.add("-Xmx2g");
        command.add("-Xss100M");
        command.add(ProcessKeys.JAVA_CLASSPATH_FLAG);
        command.add(classpath);
        command.add(className);

        // setup sub process
        processBuilder = new ProcessBuilder(command);
        processBuilder.directory( Paths.get(".").toFile() );

        Map<String, String> processEnv = processBuilder.environment();

        if ( System.getenv(Keys.SYSTEM_ENV_MAPLE) != null )
            processEnv.put(Keys.SYSTEM_ENV_MAPLE, System.getenv(Keys.SYSTEM_ENV_MAPLE));
        if ( System.getenv(Keys.SYSTEM_ENV_LD_LIBRARY_PATH) != null )
            processEnv.put(Keys.SYSTEM_ENV_LD_LIBRARY_PATH, System.getenv(Keys.SYSTEM_ENV_LD_LIBRARY_PATH));
    }

    private void waitForSuccessfulSetupSignal() throws IOException {
        LOG.info("Wait for sub process to be ready.");
        InputStream in = process.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        String line = reader.readLine();
        while ( !line.matches(".*" + READY_SIGNAL + ".*") ) {
            LOG.debug("Subprocess setup - " + line);
            line = reader.readLine();
        }
        LOG.info("Finished setting up subprocess. Redirect streams.");

        infoLogRunner = new SubprocessLoggerRunner(true, in);
        errorLogRunner = new SubprocessLoggerRunner(false, process.getErrorStream());

        new Thread(infoLogRunner).start();
        new Thread(errorLogRunner).start();
    }

    public void start() throws IOException {
        if (process != null && process.isAlive()) {
            process.destroyForcibly();
        }
        process = processBuilder.start();
        waitForSuccessfulSetupSignal();
        LOG.info("Subprocess finished successfully. Setup RMI connection.");
    }

    public void stop() {
        if ( infoLogRunner != null ) {
            infoLogRunner.interrupt();
            errorLogRunner.interrupt();
            infoLogRunner = null;
            errorLogRunner = null;
        }
    }

    public static void main(String[] args) throws IOException, NotBoundException, InterruptedException {
        RmiProcessHandler processHandler = new RmiProcessHandler(RmiProcess.class);
        processHandler.start();

        // we should be ready to interact right now...
        Registry registry = LocateRegistry.getRegistry();
        RmiShutdowner rmi = (RmiShutdowner) registry.lookup(RmiProcess.getId());
        double result = rmi.plus(2, 3);
        LOG.info("Received result: " + result);
        Thread.sleep(1500);
        LOG.info("Alright, seems to work. Time to shut it down, right?");
        processHandler.stop();
        rmi.stop();
        LOG.info("What now? Not sure... ");

        try {
            double lol = rmi.plus(1,2);
            System.out.println(lol);
            return;
        } catch (Exception e) {
            LOG.warn("Unable to perform computation on other VM. Restart other VM.");
        }

        processHandler.start();
        LOG.info("Not sure if we need a new lookup or if we can just continue? That would be cool!");
        // ok damn, if there will be an exception, we need to lookup the object again
        // makes sense... probably thats fine...
        rmi = (RmiShutdowner) registry.lookup(RmiProcess.getId());
        rmi.plus(2, 3);
        rmi.stop();
    }

    private static class SubprocessLoggerRunner implements Runnable {
        private boolean interrupt = false;

        private final boolean isInfoStream;

        private final InputStream is;

        private SubprocessLoggerRunner(boolean isInfoStream, InputStream is) {
            this.isInfoStream = isInfoStream;
            this.is = is;
        }

        @Override
        public void run() {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                while ( true ) {
                    String msg = reader.readLine();
                    if ( interrupt ) break;
                    Level level;
                    if ( isInfoStream ) {
                        level = msg.contains("DEBUG") ? Level.DEBUG : Level.INFO;
                    } else {
                        level = Level.ERROR;
                    }
                    LOG.printf(level, "Subprocess: %s", msg);
                }
                LOG.info("Stop logging subprocess");
            } catch (IOException ioe) {
                LOG.error("Unable to connect", ioe);
            }
        }

        public void interrupt() {
            this.interrupt = true;
        }
    }
}
