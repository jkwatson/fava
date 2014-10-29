package com.flightstats.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class CollectionUtils {
    public static <T> Set<T> hashSetOf(T... ts) {
        Set<T> results = new HashSet<>();
        Collections.addAll(results, ts);
        return results;
    }

    public static <K, V> HashMapBuilder<K, V> hashMapBuilder() {
        return new HashMapBuilder<>();
    }

    public static class HashMapBuilder<K, V> {
        HashMap<K, V> product = new HashMap<>();

        public HashMapBuilder<K, V> with(K key, V value) {
            product.put(key, value);
            return this;
        }

        public HashMap<K, V> build() {
            return product;
        }
    }

}
