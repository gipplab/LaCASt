package gov.nist.drmf.interpreter.maple.wrapper;

import com.maplesoft.externalcall.MapleException;
import com.maplesoft.openmaple.Algebraic;
import com.maplesoft.openmaple.EngineCallBacks;

/**
 * TODO ok the EngineCallBacks might be difficult to decouple. Dynamic proxies might be the way to go here:
 * https://www.baeldung.com/java-dynamic-proxies
 * Check this later.
 */
public class MapleEngineWrapper {
    /**
     * Maple's engine
     */
    private final com.maplesoft.openmaple.Engine engine;

    public MapleEngineWrapper(String[] args, EngineCallBacks callbacks, Object data, Object info) throws MapleException {
        this.engine = new com.maplesoft.openmaple.Engine(args, callbacks, data, info);
    }

    public Algebraic evaluate(String input) throws MapleException {
        return engine.evaluate(input);
    }

    public void restart() throws MapleException {
        this.engine.restart();
    }
}