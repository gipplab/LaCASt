package gov.nist.drmf.interpreter.common.replacements;

/**
 * @author Andre Greiner-Petter
 */
public interface IReplacementCondition extends Comparable<IReplacementCondition> {
    /**
     * Returns true if the reference condition matches this conditional object.
     * Note that this function is generally not commutative (i.e., a.match(b) unequals b.match(a)).
     * @param refCon reference condition
     * @return true if the reference condition matches this conditional object, otherwise false
     */
    boolean match(IReplacementCondition refCon);

    /**
     * Equals is not accurate for conditions. Thus, it was replaced by {@link #match(IReplacementCondition)}.
     * @param refCon reference condition
     * @return  true if the reference condition matches this condition
     */
    default boolean equals(IReplacementCondition refCon) {
        return match(refCon);
    }

    /**
     * Returns true if the reference condition {@param ref} is within the range of {@param rangeStart} to
     * {@param rangeEnd} and false otherwise.
     * @param rangeStart start range
     * @param rangeEnd end range
     * @param ref check if within range
     * @return true if {@param ref} is between {@param rangeStart} and {@param rangeEnd}
     */
    public static boolean withinRange(IReplacementCondition rangeStart, IReplacementCondition rangeEnd, IReplacementCondition ref) {
        if ( rangeStart.compareTo(rangeEnd) == 0 ) return rangeStart.compareTo(ref) == 0;
        if ( rangeStart.compareTo(rangeEnd) > 0 ) return withinRange(rangeEnd, rangeStart, ref);
        else return rangeStart.compareTo(ref) <= 0 && ref.compareTo(rangeEnd) <= 0;
    }
}
