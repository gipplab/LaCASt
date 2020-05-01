package gov.nist.drmf.interpreter.mlp.data;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Andre Greiner-Petter
 */
public class Stats {

    private int[] counter;
    private int casIdx = 1;
    private Map<String, Integer> idxMap;

    public Stats(int numOfCAS) {
        counter = new int[1+numOfCAS];
        idxMap = new HashMap<>();
    }

    public void reset() {
        Arrays.fill(counter, 0);
        idxMap.clear();
    }

    public void tickDLMF() {
        counter[0]++;
    }

    public void tickCAS(String cas) {
        Integer idx = idxMap.computeIfAbsent( cas, key -> casIdx++ );
        counter[idx]++;
    }

    public int getCountDLMF(){
        return counter[0];
    }

    /**
     * Gets the counter for a given cas
     * @param cas computer algebra sys name
     * @return counter for cas
     * @throws IndexOutOfBoundsException if given cas does not contain counter
     */
    public int getCountCAS(String cas) throws IndexOutOfBoundsException {
        return counter[idxMap.getOrDefault( cas, -1 )];
    }
}
