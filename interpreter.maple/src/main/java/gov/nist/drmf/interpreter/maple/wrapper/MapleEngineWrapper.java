package gov.nist.drmf.interpreter.maple.wrapper;

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
        try {
            this.engine = new com.maplesoft.openmaple.Engine(args, callbacks, data, info);
        } catch (Exception e) {
            throw new MapleException(e);
        }
    }

    public Algebraic evaluate(String input) throws MapleException {
        try {
            return OpenMapleWrapperHelper.delegateOpenMapleObject(
                    engine.evaluate(input)
            );
        } catch (Exception e) {
            throw new MapleException(e);
        }
    }

    public void restart() throws MapleException {
        try {
            this.engine.restart();
        } catch (Exception e) {
            throw new MapleException(e);
        }
    }
}