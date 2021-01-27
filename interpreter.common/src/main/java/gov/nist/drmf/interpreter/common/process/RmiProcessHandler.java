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
import java.util.*;
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

    private final HashMap<Long, Thread> shutdownHookProcessMap;
    private final HashSet<Long> ignorePidShutdown;

    private CompletableFuture<Process> completeProcessFuture;

    public RmiProcessHandler(RmiSubprocessInfo info) {
        List<String> command = info.getCommandLineArguments();

        // setup sub process
        processBuilder = new ProcessBuilder(command);
        processBuilder.directory( Paths.get(".").toFile() );

        Map<String, String> processEnv = processBuilder.environment();
        processBuilder.redirectErrorStream(true);

        if ( System.getenv(Keys.SYSTEM_ENV_MAPLE) != null )
            processEnv.put(Keys.SYSTEM_ENV_MAPLE, System.getenv(Keys.SYSTEM_ENV_MAPLE));
        if ( System.getenv(Keys.SYSTEM_ENV_LD_LIBRARY_PATH) != null )
            processEnv.put(Keys.SYSTEM_ENV_LD_LIBRARY_PATH, System.getenv(Keys.SYSTEM_ENV_LD_LIBRARY_PATH));

        this.shutdownHookProcessMap = new HashMap<>();
        this.ignorePidShutdown = new HashSet<>();
    }

    private void waitForSuccessfulSetupSignal() throws IOException {
        LOG.info("Wait for sub process to be ready.");
        InputStream in = process.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        String line = reader.readLine();
        while ( !line.matches(".*" + READY_SIGNAL + ".*") ) {
            LOG.info("Subprocess setup - " + line);
            line = reader.readLine();
            if ( line == null ) {
                throw new IOException("Subprocess is unavailable or does not send ready signal properly.");
            }
        }

        logRunner = new SubprocessLoggerRunner(process.getInputStream());
        logRunnerThread = new Thread(logRunner);
        logRunnerThread.setDaemon(true);
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

        // add shutdown hook to every process...
        Thread shutdownHook = new Thread(() -> stopProcess(process));
        Runtime.getRuntime().addShutdownHook(shutdownHook);
        shutdownHookProcessMap.put(process.pid(), shutdownHook);

        waitForSuccessfulSetupSignal();

        LOG.info("Established connection with sub process. Setup restart on fail hook.");
        this.completeProcessFuture = process.onExit().thenApply(this::onCrash);

        LOG.info("Subprocess finished successfully. Setup RMI connection.");
    }

    private void stopProcess(Process process) {
        if ( process != null ) {
            // before stopping, we need to tell our still active recovery fallback to ignore this specific PID
            LOG.info("Shutdown hook triggered. Forcefully shutting down subprocess [" + process.pid() +"]");
            this.ignorePidShutdown.add(process.pid());
            process.destroyForcibly();
        }
    }

    public void stop() {
        LOG.debug("Stop logger and running processes");
        if ( logRunner != null ) {
            logRunner.interrupt();
            logRunnerThread.interrupt();
        }

        if ( process != null && process.isAlive() ) {
            LOG.info("Process is running. Force shutdown [" + process.pid() + "]");
            ignorePidShutdown.add(process.pid());
            process.destroyForcibly();
        }

        if ( process != null ) {
            try {
                LOG.debug("Remove shutdown hook for dead process [" + process.pid() + "]");
                Thread t = shutdownHookProcessMap.remove(process.pid());
                if ( t != null ) {
                    Runtime.getRuntime().removeShutdownHook(t);
                    t.interrupt();
                }
            } catch (Exception e) {
                LOG.warn("Try to remove shutdown hook for dying process but it didn't work. " + e.getMessage());
            }
        }
    }

    public boolean isAlive() {
        return process != null && process.isAlive();
    }

    public CompletableFuture<?> getProcessFuture() {
        return completeProcessFuture;
    }

    private Process onCrash(Process process) {
        if ( ignorePidShutdown.contains(process.pid()) ) {
            LOG.info("Ignore recovery fallback for forced shutdown from client-site for process id " + process.pid());
            return process;
        }

        LOG.info("Subprocess ["+process.pid()+"] stopped unexpectedly. Analyze reason...");
        int exitValue = process.exitValue();
        if ( exitValue != 0 ) {
            LOG.error("Subprocess ["+process.pid()+"] finished on error code " + exitValue + ". Try to recover by restarting VM.");
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
        RmiProcessHandler processHandler = new RmiProcessHandler(new RmiSubprocessInfo() {
            @Override
            public String getClassName() {
                return RmiProcess.class.getName();
            }

            @Override
            public List<String> getJvmArgs() {
                return List.of("");
            }
        });
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
