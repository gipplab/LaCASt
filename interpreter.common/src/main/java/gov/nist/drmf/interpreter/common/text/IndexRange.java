package gov.nist.drmf.interpreter.common.text;

/**
 * @author Andre Greiner-Petter
 */
public class IndexRange {
    private final int start, end;

    public IndexRange(int start, int end) {
        this.start = start;
        this.end = end;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }
}
