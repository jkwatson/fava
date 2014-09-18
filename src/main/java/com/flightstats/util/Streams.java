package com.flightstats.util;

import java.util.stream.Stream;

public class Streams {

    /**
     * Generates a Stream of nulls, with as many entries as you specify. Useful for doing parallel "loops" like this:
     * <code>
     *     times(1000).parallel().map(x -> generateSomethingComputationallyExpensive());
     * </code>
     * or:
     * <code>
     *     times(1000).parallel().forEach(x -> doSomethingComputationallyExpensive());
     * </code>
     */
    @SuppressWarnings("RedundantCast")
    public static Stream<Void> times(int number) {
        return Stream.generate(() -> (Void) null).limit(number);
    }


}
