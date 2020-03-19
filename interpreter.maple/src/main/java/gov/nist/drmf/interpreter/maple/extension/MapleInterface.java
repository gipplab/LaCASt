package gov.nist.drmf.interpreter.maple.extension;

import com.maplesoft.externalcall.MapleException;
import com.maplesoft.openmaple.Algebraic;
import com.maplesoft.openmaple.Engine;
import gov.nist.drmf.interpreter.maple.listener.MapleListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Observer;

/**
 * @author Andre Greiner-Petter
 */
public class MapleInterface {
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

    private List<String> procedureBackup;

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
     * Invokes the GC of Maple
     * @throws MapleException may throw an exception
     */
    public void invokeGC() throws MapleException {
        LOG.debug("Manually invoke Maple's garbage collector.");
        engine.evaluate("gc();");
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
                mapleInterface = new MapleInterface();
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

    /**
     * Quick check if Maple is available.
     * @return true if maple is running and available
     */
    public static boolean isMaplePresent() {
        try {
            return getUniqueMapleInterface() != null;
        } catch ( Exception | Error e ) {
            LOG.warn("Cannot init maple interface", e);
            return false;
        }
    }
}
