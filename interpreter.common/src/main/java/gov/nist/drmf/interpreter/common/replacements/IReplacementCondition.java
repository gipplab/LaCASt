package gov.nist.drmf.interpreter.common.replacements;

/**
 * Conditional replacement objects are comparable objects and matchable. This means,
 * another conditional object could {@link #match(IReplacementCondition)} other conditional objects.
 * Note that a match is not necessarily commutative, i.e., a conditional object A could match B, but B
 * does not match A.
 *
 * To provide an example, the {@link DLMFConditionalReplacementImpl} is an implementation of this interface.
 * A DLMF condition is an equation label, e.g., 9.6#E3. Consider a replacement rule for all equations in
 * subsection 9.6. In this case, every equation in subsection 9.6, such as 9.6#E3 matches 9.6. However,
 * obviously, 9.6 does not match 9.6#E3.
 *
 * @see DLMFConditionalReplacementImpl
 * @see ConditionalReplacementRule
 * @see Comparable
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
     * Returns true if the reference condition {@param ref} is within the range of {@param rangeStart} to
     * {@param rangeEnd} and false otherwise.
     * @param rangeStart start range
     * @param rangeEnd end range
     * @param ref check if within range
     * @return true if {@param ref} is between {@param rangeStart} and {@param rangeEnd}
     */
    static boolean withinRange(IReplacementCondition rangeStart, IReplacementCondition rangeEnd, IReplacementCondition ref) {
        if ( rangeStart.compareTo(rangeEnd) == 0 ) return rangeStart.compareTo(ref) == 0;
        if ( rangeStart.compareTo(rangeEnd) > 0 ) return withinRange(rangeEnd, rangeStart, ref);
        else return rangeStart.compareTo(ref) <= 0 && ref.compareTo(rangeEnd) <= 0;
    }
}
