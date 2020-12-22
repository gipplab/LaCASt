package gov.nist.drmf.interpreter.common.meta;

import java.util.List;
import java.util.function.Predicate;

import static java.util.function.Predicate.not;

/**
 * @author Andre Greiner-Petter
 */
public final class ListExtender {
    private ListExtender(){}

    /**
     * Adds elements that do not already exist. It's similar to set but still keeping the list
     * logic.
     * @param list will be changed by new elements
     * @param addingList the elements will be added to list of not already exist
     * @param <T> the type of both list elements
     */
    public static <T> void addIfNotExist(List<T> list, List<T> addingList) {
        addAll( list, addingList, not(list::contains) );
    }

    /**
     * Add all expressions from addingList to list that obey the given condition.
     * @param list the list that will change
     * @param addingList the list with elements that will added to list
     * @param condition the condition the elements must obey to be added
     * @param <T> list must be of the same type
     */
    public static <T> void addAll(List<T> list, List<T> addingList, Predicate<T> condition) {
        addingList.stream()
                .filter( condition )
                .forEach( list::add );
    }
}
