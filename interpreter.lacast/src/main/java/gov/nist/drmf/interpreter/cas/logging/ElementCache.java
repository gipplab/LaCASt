package gov.nist.drmf.interpreter.cas.logging;

import java.util.List;

/**
 * @author Andre Greiner-Petter
 */
public class ElementCache {

    private boolean prevElementEndsWithMultiply, prevElementInnerCache;

    ElementCache(boolean prevElementEndsWithMultiply, boolean prevElementInnerCache) {
        this.prevElementEndsWithMultiply = prevElementEndsWithMultiply;
        this.prevElementInnerCache = prevElementInnerCache;
    }

    public boolean isPrevElementEndsWithMultiply() {
        return prevElementEndsWithMultiply;
    }

    public void setPrevElementEndsWithMultiply(boolean prevElementEndsWithMultiply) {
        this.prevElementEndsWithMultiply = prevElementEndsWithMultiply;
    }

    public boolean isPrevElementInnerCache() {
        return prevElementInnerCache;
    }

    public void setPrevElementInnerCache(boolean prevElementInnerCache) {
        this.prevElementInnerCache = prevElementInnerCache;
    }
}
