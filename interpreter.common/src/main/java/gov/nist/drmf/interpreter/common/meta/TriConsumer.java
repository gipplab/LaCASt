package gov.nist.drmf.interpreter.common.meta;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Essentially an extension of the functional interface {@link java.util.function.BiConsumer}.
 * While the {@link java.util.function.BiConsumer} is a two-arity specialization of {@link Consumer},
 * this is three-arity specialization of {@link Consumer}.
 *
 * @param <T> the type of the first argument to the operation
 * @param <U> the type of the second argument to the operation
 * @param <V> the type of the third argument to the operation
 */
@FunctionalInterface
public interface TriConsumer<T, U, V> {
    /**
     * See {@link java.util.function.BiConsumer#accept(Object, Object)} for more information. This is the same but with
     * three arguments
     * @param t the first input argument
     * @param u the first input argument
     * @param v the first input argument
     */
    void accept(T t, U u, V v);

    /**
     * See {@link java.util.function.BiConsumer#andThen(BiConsumer)} for more information. This is the same but with
     * three arguments
     * @param after the operation to perform after this operation
     * @return a composed {@code TriConsumer} that performs in sequence this
     * operation followed by the {@code after} operation
     * @throws NullPointerException if {@code after} is null
     */
    default TriConsumer<T, U, V> andThen(TriConsumer<? super T, ? super U, ? super V> after) {
        Objects.requireNonNull(after);
        return (l, r, s) -> {
            accept(l, r, s);
            after.accept(l, r, s);
        };
    }
}
