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
import java.util.concurrent.CompletableFuture;

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

    private SubprocessLoggerRunner logRunner;
    private Thread logRunnerThread;

    private CompletableFuture<Process> completeProcessFuture;

    public RmiProcessHandler(Class<?> clazz, List<String> jvmArgs) {
        // setup commands
        String javaHome = System.getProperty(ProcessKeys.JAVA_HOME);
        String javaBin = Paths.get(javaHome, "bin", "java").toString();
        String classpath = System.getProperty(ProcessKeys.JAVA_CLASSPATH);
        String className = clazz.getName();

        List<String> command = new LinkedList<>();
        command.add(javaBin);
        command.addAll(jvmArgs);
        command.add(ProcessKeys.JAVA_CLASSPATH_FLAG);
        command.add(classpath);
        command.add(className);

        // setup sub process
        processBuilder = new ProcessBuilder(command);
        processBuilder.directory( Paths.get(".").toFile() );

        Map<String, String> processEnv = processBuilder.environment();
        processBuilder.redirectErrorStream(true);

        if ( System.getenv(Keys.SYSTEM_ENV_MAPLE) != null )
            processEnv.put(Keys.SYSTEM_ENV_MAPLE, System.getenv(Keys.SYSTEM_ENV_MAPLE));
        if ( System.getenv(Keys.SYSTEM_ENV_LD_LIBRARY_PATH) != null )
            processEnv.put(Keys.SYSTEM_ENV_LD_LIBRARY_PATH, System.getenv(Keys.SYSTEM_ENV_LD_LIBRARY_PATH));

        Thread shutdownHook = new Thread(this::stop);
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    private void waitForSuccessfulSetupSignal() throws IOException {
        LOG.info("Wait for sub process to be ready.");
        InputStream in = process.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        String line = reader.readLine();
        while ( !line.matches(".*" + READY_SIGNAL + ".*") ) {
            LOG.debug("Subprocess setup - " + line);
            line = reader.readLine();
            if ( line == null )
                throw new IOException("Subprocess is unavailable or does not send ready signal properly.");
        }

        logRunner = new SubprocessLoggerRunner(process.getInputStream());
        logRunnerThread = new Thread(logRunner);
        logRunnerThread.start();

        LOG.debug("Received ready signal from subprocess. Initialization has finished successfully.");
    }

    public void start() throws IOException {
        if (process != null && process.isAlive()) {
            LOG.warn("Old process is still running. We won't restart a new one.");
            return;
        }

        LOG.info("Start new sub process");
        process = processBuilder.start();
        waitForSuccessfulSetupSignal();

        LOG.info("Established connection with sub process. Setup restart on fail hook.");
        this.completeProcessFuture = process.onExit().thenApply(this::onCrash);

        LOG.info("Subprocess finished successfully. Setup RMI connection.");
    }

    public void stop() {
        LOG.debug("Stop logger and running processes");
        if ( logRunner != null ) {
            logRunner.interrupt();
            logRunnerThread.interrupt();
        }

        if ( process != null && process.isAlive() ) {
            LOG.info("Shutdown subprocess");
            process.destroyForcibly();
        }
    }

    public CompletableFuture<?> getProcessFuture() {
        return completeProcessFuture;
    }

    private Process onCrash(Process process) {
        LOG.info("Subprocess stopped. Analyze reason...");
        int exitValue = process.exitValue();
        if ( exitValue != 0 ) {
            LOG.error("Subprocess finished on error code " + exitValue + ". Try to recover by restarting VM.");
            this.stop();
            try {
                this.start();
                return process;
            } catch (IOException e) {
                LOG.error("Unable to recover subprocess!", e);
            }
        } else {
            LOG.info("Subprocess stopped properly. No crash that needs to get recovered.");
        }
        return null;
    }

    public static void main(String[] args) throws IOException, NotBoundException, InterruptedException {
        RmiProcessHandler processHandler = new RmiProcessHandler(RmiProcess.class, List.of(""));
        processHandler.start();

        // we should be ready to interact right now...
        Registry registry = LocateRegistry.getRegistry();
        RmiShutdowner rmi = (RmiShutdowner) registry.lookup(RmiProcess.getId());
        try {
            double result = rmi.plus(2, 3);
            LOG.info("Lol, just worked smoothly: " + result);
        } catch (Exception e) {
            LOG.error("Catched it", e);
//            LOG.warn("Still alive? " + processHandler.process.isAlive());
//            LOG.warn("What was exit code? " + processHandler.process.exitValue());
        }

        Thread.sleep(2000);
        LOG.info("Maybe the storm is over now. To to look for object and call stop properly.");
        rmi = (RmiShutdowner) registry.lookup(RmiProcess.getId());
        processHandler.stop();
        rmi.stop();

//        LOG.info("Received result: " + result);
//        Thread.sleep(1500);
//        LOG.info("Alright, seems to work. Time to shut it down, right?");
//        processHandler.stop();
//        rmi.stop();
//        LOG.info("What now? Not sure... ");
//
//        try {
//            double lol = rmi.plus(1,2);
//            System.out.println(lol);
//            return;
//        } catch (Exception e) {
//            LOG.warn("Unable to perform computation on other VM. Restart other VM.");
//        }
//
//        processHandler.start();
//        LOG.info("Not sure if we need a new lookup or if we can just continue? That would be cool!");
//        // ok damn, if there will be an exception, we need to lookup the object again
//        // makes sense... probably thats fine...
//        rmi = (RmiShutdowner) registry.lookup(RmiProcess.getId());
//        rmi.plus(2, 3);
//        rmi.stop();
    }

    public static void sendReadySignal() {
        System.out.println(RmiProcessHandler.READY_SIGNAL);
    }

    private static class SubprocessLoggerRunner implements Runnable {
        private boolean interrupt = false;

        private final InputStream is;

        private SubprocessLoggerRunner(InputStream is) {
            this.is = is;
        }

        @Override
        public void run() {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                String msg = reader.readLine();
                while ( msg != null ) {
                    if ( interrupt ) break;
                    LOG.printf(getLevel(msg), "Subprocess: %s", msg);
                    msg = reader.readLine();
                }
                LOG.info("Stop logging subprocess");
            } catch (IOException ioe) {
                LOG.error("Unable to connect", ioe);
            }
        }

        private Level getLevel(String msg) {
            Level level = Level.INFO;
            if ( msg.contains("TRACE") ) level = Level.TRACE;
            else if ( msg.contains("DEBUG") ) level = Level.DEBUG;
            else if ( msg.contains("WARN") ) level = Level.WARN;
            else if ( msg.matches(".*(ERROR|FATAL).*") ) level = Level.ERROR;
            return level;
        }

        public void interrupt() {
            this.interrupt = true;
        }
    }
}
