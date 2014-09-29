package com.flightstats.util;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static junit.framework.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class StreamsTest {

    @Test
    public void testTimes() throws Exception {
        AtomicInteger resultCount = new AtomicInteger(0);
    	Streams.times(100).forEach(x -> {
            assertNull(x);
            resultCount.incrementAndGet();
        });
        assertEquals(100, resultCount.get());
    }

    @Test
    public void testTimes_parallelism() throws Exception {
        AtomicInteger resultCount = new AtomicInteger(0);
        List<Integer> counter = Collections.synchronizedList(new ArrayList<>());
        Streams.times(100).parallel().forEach(x -> {
            assertNull(x);
            int i = resultCount.getAndIncrement();
            counter.add(i);
        });
        assertEquals(100, resultCount.get());
        int current = 0;
        AtomicBoolean alwaysInOrder = new AtomicBoolean(true);
        for (Integer integer : counter) {
            if (integer != current++) {
                alwaysInOrder.set(false);
            }
        }

        assertFalse(alwaysInOrder.get());
    }

}