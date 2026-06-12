package com.minicache.store;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * KeyValueStore is the core data structure of MiniCache.
 *
 * Why ConcurrentHashMap?
 * - Phase 2 will add a TCP server handling MULTIPLE clients at once,
 *   each on its own thread. A plain HashMap is NOT thread-safe and
 *   would corrupt data or throw exceptions under concurrent access.
 * - ConcurrentHashMap allows safe concurrent reads/writes without us
 *   manually managing locks for basic operations.
 */
public class KeyValueStore {

    private final Map<String, String> data;

    public KeyValueStore() {
        this.data = new ConcurrentHashMap<>();
    }

    public void set(String key, String value) {
        data.put(key, value);
    }

    public String get(String key) {
        return data.get(key);
    }

    public boolean del(String key) {
        return data.remove(key) != null;
    }

    public boolean exists(String key) {
        return data.containsKey(key);
    }

    public int size() {
        return data.size();
    }
}