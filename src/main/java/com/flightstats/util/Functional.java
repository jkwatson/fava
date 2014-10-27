package com.flightstats.util;

import java.util.Collection;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class Functional {

    /**
     * Generates a Stream of nulls, with as many entries as you specify. Useful for doing parallel "loops" like this:
     * <code>
     * times(1000).parallel().map(x -&gt; generateSomethingComputationallyExpensive());
     * </code>
     * or:
     * <code>
     * times(1000).parallel().forEach(x -&gt; doSomethingComputationallyExpensive());
     * </code>
     */
    @SuppressWarnings("RedundantCast")
    public static Stream<Void> times(int number) {
        return IntStream.range(0, number).mapToObj(i -> (Void) null);
    }

    /**
     * Run the supplied Runnable n times.
     */
    public static void times(int n, Runnable r) {
        times(n).forEach(x -> r.run());
    }

    /**
     * Run the supplied Runnable n times, in parallel. The parallelism is determined here by the same criteria
     * as Collections.parallelStream();
     */
    public static void timesParallel(int n, Runnable r) {
        times(n).parallel().forEach(x -> r.run());
    }

    /**
     * Call the supplied callable n times, placing the returned value into a collection.
     */
    public static <T> Collection<T> times(int n, NoThrowCallable<T> callable) {
        return times(n).map(x -> callable.call()).collect(toList());
    }

    /**
     * Call the supplied callable n times, placing the returned value into a collection.
     * The parallelism is determined here by the same criteria as Collections.parallelStream();
     */
    public static <T> Collection<T> timesParallel(int n, NoThrowCallable<T> callable) {
        return times(n).parallel().map(x -> callable.call()).collect(toList());
    }

    /**
     * Equivalent to java.util.concurrent.Callable, except with no Exception on the method signature.
     */
    public static interface NoThrowCallable<T> {
        T call();
    }

}
