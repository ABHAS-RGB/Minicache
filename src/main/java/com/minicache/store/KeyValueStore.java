package com.minicache.store;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class KeyValueStore {

    private final Map<String, String> data;
    private final Map<String, Long> expiryTimes;

    public KeyValueStore() {
        this.data = new ConcurrentHashMap<>();
        this.expiryTimes = new ConcurrentHashMap<>();
    }

    public void set(String key, String value) {
        data.put(key, value);
        expiryTimes.remove(key);
    }

    public String get(String key) {
        if (isExpired(key)) {
            removeKey(key);
            return null;
        }
        return data.get(key);
    }

    public boolean del(String key) {
        if (isExpired(key)) {
            removeKey(key);
            return false;
        }
        boolean removed = data.remove(key) != null;
        expiryTimes.remove(key);
        return removed;
    }

    public boolean exists(String key) {
        if (isExpired(key)) {
            removeKey(key);
            return false;
        }
        return data.containsKey(key);
    }

    public boolean expire(String key, long seconds) {
        if (isExpired(key) || !data.containsKey(key)) {
            removeKey(key);
            return false;
        }
        if (seconds <= 0) {
            removeKey(key);
            return true;
        }
        long expiryTimestamp = System.currentTimeMillis() + (seconds * 1000L);
        expiryTimes.put(key, expiryTimestamp);
        return true;
    }

    public long ttl(String key) {
        if (isExpired(key)) {
            removeKey(key);
            return -2;
        }
        if (!data.containsKey(key)) {
            return -2;
        }
        Long expiryTimestamp = expiryTimes.get(key);
        if (expiryTimestamp == null) {
            return -1;
        }
        long remainingMillis = expiryTimestamp - System.currentTimeMillis();
        return Math.max(0, (remainingMillis + 999) / 1000);
    }

    private boolean isExpired(String key) {
        Long expiryTimestamp = expiryTimes.get(key);
        if (expiryTimestamp == null) {
            return false;
        }
        return System.currentTimeMillis() >= expiryTimestamp;
    }

    private void removeKey(String key) {
        data.remove(key);
        expiryTimes.remove(key);
    }

    public int removeExpiredKeys() {
        int removed = 0;
        long now = System.currentTimeMillis();
        for (String key : expiryTimes.keySet().toArray(new String[0])) {
            Long expiryTimestamp = expiryTimes.get(key);
            if (expiryTimestamp != null && now >= expiryTimestamp) {
                removeKey(key);
                removed++;
            }
        }
        return removed;
    }

    public int size() {
        return data.size();
    }

    /**
     * Returns an unmodifiable snapshot of the data map for persistence.
     * Unmodifiable = PersistenceManager can read it but not accidentally
     * modify the live store while iterating.
     */
    public Map<String, String> getDataSnapshot() {
        return Collections.unmodifiableMap(data);
    }

    /**
     * Returns an unmodifiable snapshot of the expiry map for persistence.
     */
    public Map<String, Long> getExpirySnapshot() {
        return Collections.unmodifiableMap(expiryTimes);
    }
}