package com.flightstats.util;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.junit.Assert.*;

public class FunctionalTest {

    @Test
    public void testTimes() throws Exception {
        AtomicInteger resultCount = new AtomicInteger(0);
        Functional.times(100).forEach(x -> {
            assertNull(x);
            resultCount.incrementAndGet();
        });
        assertEquals(100, resultCount.get());
    }

    @Test
    public void testTimes_parallelism() throws Exception {
        boolean seenOutOfOrder = false;
        for (int j = 0; !seenOutOfOrder && (j < 100); j++) {
            Random random = new Random();
            AtomicInteger resultCount = new AtomicInteger(0);
            List<Integer> counter = Collections.synchronizedList(new ArrayList<>());
            Functional.times(100).parallel().forEach(x -> {
                try {
                    Thread.sleep(random.nextInt(10));
                    assertNull(x);
                    int i = resultCount.getAndIncrement();
                    counter.add(i);
                } catch (InterruptedException e) {
                    //ignore this..test will fail in this case anyway.
                }
            });
            assertEquals(100, resultCount.get());
            for (int i = 0; i < counter.size(); i++) {
                 if(counter.get(i) != i){
                     seenOutOfOrder = true;
                 }

            }
        }
        assertTrue(seenOutOfOrder);
    }

    @Test
    public void testMemory() throws Exception {
        //just verifying that this doesn't blow the heap to do the largest one possible.
        Stream<Void> times = Functional.times(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, times.count());
    }

}