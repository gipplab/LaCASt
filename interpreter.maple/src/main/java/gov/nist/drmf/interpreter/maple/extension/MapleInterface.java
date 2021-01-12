package gov.nist.drmf.interpreter.maple.extension;

import com.maplesoft.externalcall.MapleException;
import com.maplesoft.openmaple.Algebraic;
import com.maplesoft.openmaple.Engine;
import com.maplesoft.openmaple.MString;
import gov.nist.drmf.interpreter.common.cas.IComputerAlgebraSystemEngine;
import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import gov.nist.drmf.interpreter.maple.common.MapleConfig;
import gov.nist.drmf.interpreter.maple.listener.MapleListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Observer;

/**
 * @author Andre Greiner-Petter
 */
public final class MapleInterface implements IComputerAlgebraSystemEngine {
    private static final Logger LOG = LogManager.getLogger(MapleInterface.class.getName());

    /**
     * Inner constant to initialize Maple
     */
    private static final String[] maple_args = new String[]{
            "java"
    };

    /**
     * Maple's engine
     */
    private Engine engine;

    /**
     * Maple listener
     */
    private static MapleListener listener;

    /**
     * The unique instance
     */
    private static MapleInterface mapleInterface;

    /**
     *
     */
    private final List<String> procedureBackup;

    /**
     * The signal maple returns if the computation timed out
     */
    public static final String TIMED_OUT_SIGNAL = "TIMED-OUT";

    private boolean loadedQExtension = false;

    /**
     * The interface to maple
     * @throws MapleException if init wont work
     */
    private MapleInterface() throws MapleException {
        procedureBackup = new LinkedList<>();
        init();
    }

    /**
     * Instantiate the interface to Maple. If it is already
     * instantiate, nothing will happen. You can get the instance
     * by invoke {@link #getUniqueMapleInterface()}.
     *
     * The initialization process can be split into four parts.
     *  1)  It loads the Maple procedure from libs/ReferenceData/MapleProcedures
     *      to convert the inert-form of a Maple expression to a
     *      Maple list of the inert-form. This could produces an {@link IOException}
     *      if it is not possible to load the procedure from the file.
     *
     *  2)  It starts the Maple engine. This could produces a {@link MapleException}
     *      if the initialization of Maple's engine fails or the evaluation
     *      of the loaded procedure fails.
     *
     *  3)  It loads the necessary translation files to translate greek letters,
     *      mathematical constants and functions. Since it loads those translations
     *      from files, this part can produces an {@link IOException} again.
     *
     * @throws MapleException   if it is not possible to initiate the {@link Engine}
     *                          from the openmaple API or the evaluation of the
     *                          pre-defined Maple procedure fails.
     */
    private void init() throws MapleException {
        listener = new MapleListener(true);
        engine = new Engine( maple_args, listener, null, null );
    }

    /**
     * For tests
     * @return the engine of Maple
     */
    Engine getEngine() {
        return engine;
    }

    /**
     * It evaluates the given expression via Maple.
     * Make sure your expression ends with a semicolon.
     * Otherwise you will produce a MapleException.
     * @param input syntactical correct Maple expression ended with a semicolon
     * @return the algebraic object of the result.
     * @throws MapleException if Maple produces an error
     */
    public Algebraic evaluate(String input) throws MapleException {
        return engine.evaluate(input);
    }

    @Override
    public String enterCommand(String command) throws ComputerAlgebraSystemEngineException {
        try {
            return evaluate(command).toString();
        } catch (MapleException me) {
            throw new ComputerAlgebraSystemEngineException(me);
        }
    }

    /**
     * Invokes the GC of Maple
     * @throws MapleException may throw an exception
     */
    public void invokeGC() throws MapleException {
        LOG.debug("Manually invoke Maple's garbage collector.");
        engine.evaluate("gc();");
    }

    @Override
    public void forceGC() throws ComputerAlgebraSystemEngineException {
        try {
            invokeGC();
        } catch (MapleException e) {
            throw new ComputerAlgebraSystemEngineException(e);
        }
    }

    @Override
    public String buildList(List<String> list) {
        return buildMapleList(list);
    }

    public static String buildMapleList(List<String> list) {
        if ( list == null ) {
            return null;
        }
        String listStr = CommandBuilder.makeMapleList(list);
        if ( listStr != null && listStr.length() > 3 )
            return listStr.substring(1, listStr.length()-1);
        return listStr;
    }

    /**
     * Adds an observer to the listener
     * @param observer observer
     */
    @Deprecated
    public void addMemoryObserver(Observer observer) {
        listener.addObserver(observer);
    }

    /**
     * Loads a procedure
     * @param procedure
     * @throws MapleException
     */
    public void loadProcedure( String procedure ) throws MapleException {
        engine.evaluate( procedure );
        procedureBackup.add(procedure);
    }

    /**
     * Restarts the Maple session and reloads the Maple procedures.
     * Note that you have to reload your own settings again after a
     * restart of the engine.
     *
     * @throws MapleException if the engine cannot be restarted
     * @throws IOException if the procedures cannot be loaded
     */
    public void restart() throws MapleException {
        engine.restart();
        for ( String proc : procedureBackup )
            evaluate(proc);
    }

    /**
     * Returns the unique interface to Maple. This might be
     * null, if you haven't invoke {@link #init()} previously!
     *
     * @return the unique object of the interface to Maple. Can
     * be null.
     */
    public static MapleInterface getUniqueMapleInterface() {
        if ( mapleInterface == null ) {
            try {
                if (MapleConfig.isMapleSetup()) mapleInterface = new MapleInterface();
                return mapleInterface;
            } catch (MapleException e) {
                LOG.error("Unable to load ");
                return null;
            }
        }
        return mapleInterface;
    }

    /**
     * Return the unique listener of current Maple process.
     *
     * @return unique listener
     */
    public static MapleListener getUniqueMapleListener(){ return listener; }

    public boolean isAbortedExpression(Algebraic result) {
        if ( result instanceof MString) {
            try {
                return ((MString) result).stringValue().equals(MapleInterface.TIMED_OUT_SIGNAL);
            } catch (MapleException e) {
                LOG.error("A maple exception occurred when testing the result " + result);
                return false;
            }
        }
        return false;
    }

    /**
     * Checks if the given command returns an integer that is in the specified range.
     * Throws an exception if the check fails!
     * @param command the command that has to be evaluated
     * @param lowerLimit the lower limit (included)
     * @param upperLimit the upper limit (included)
     * @throws IllegalArgumentException if the given command did not return an integer in the given range
     * @throws ComputerAlgebraSystemEngineException if the command cannot be evaluated
     */
    public int evaluateAndCheckRangeOfResult(String command, int lowerLimit, int upperLimit)
            throws IllegalArgumentException, ComputerAlgebraSystemEngineException {
        try {
//            LOG.debug("Prepare numerical test:" + NL + command);
            Algebraic numCombis = evaluate(command);
            try {
                int i = Integer.parseInt(numCombis.toString());
                if ( i >= upperLimit ) throw new IllegalArgumentException("Too many combinations: " + i);
                else if ( i <= lowerLimit ) throw new IllegalArgumentException("There are no valid test values.");
                return i;
            } catch ( NumberFormatException e ){
                throw new IllegalArgumentException("Cannot calculate number of combinations!");
            }
        } catch (MapleException me) {
            throw new ComputerAlgebraSystemEngineException(me);
        }
    }
}
