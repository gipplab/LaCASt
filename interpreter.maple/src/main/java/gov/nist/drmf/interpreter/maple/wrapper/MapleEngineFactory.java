package gov.nist.drmf.interpreter.maple.wrapper;

import gov.nist.drmf.interpreter.common.exceptions.CASUnavailableException;
import gov.nist.drmf.interpreter.maple.listener.MapleListener;
import gov.nist.drmf.interpreter.maple.wrapper.openmaple.Engine;

/**
 * @author Andre Greiner-Petter
 */
public abstract class MapleEngineFactory {
    /**
     * Inner constant to initialize Maple
     */
    private static final String[] maple_args = new String[]{
            "java"
    };

    /**
     * Maple listener
     */
    private static final MapleListener listener = new MapleListener(true);

    /**
     * The singleton Engine since there cannot be more than one.
     */
    private static Engine singletonEngine = null;

    /**
     * Get the unique engine engine instance
     * @return get Maple's engine
     * @throws CASUnavailableException if Maple is unavailable
     */
    public static Engine getEngineInstance() throws CASUnavailableException {
        if ( singletonEngine == null )
            singletonEngine = EngineHelper.getNewEngine(maple_args, listener, null, null);
        return singletonEngine;
    }

    public static MapleListener getUniqueMapleListener() {
        return listener;
    }
}
