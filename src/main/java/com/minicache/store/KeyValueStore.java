package com.minicache.store;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class KeyValueStore {

    private final Map<String, String> data;
    private final Map<String, Long> expiryTimes;
    private final Map<String, List<String>> lists;
    private final Map<String, Set<String>> sets;
    private final Map<String, Map<String, String>> hashes;

    public KeyValueStore() {
        this.data = new ConcurrentHashMap<>();
        this.expiryTimes = new ConcurrentHashMap<>();
        this.lists = new ConcurrentHashMap<>();
        this.sets = new ConcurrentHashMap<>();
        this.hashes = new ConcurrentHashMap<>();
    }

    public void set(String key, String value) {
        data.put(key, value);
        expiryTimes.remove(key);
    }

    public String get(String key) {
        if (isExpired(key)) { removeKey(key); return null; }
        return data.get(key);
    }

    public boolean del(String key) {
        if (isExpired(key)) { removeKey(key); return false; }
        boolean removed = data.remove(key) != null;
        removed |= lists.remove(key) != null;
        removed |= sets.remove(key) != null;
        removed |= hashes.remove(key) != null;
        expiryTimes.remove(key);
        return removed;
    }

    public boolean exists(String key) {
        if (isExpired(key)) { removeKey(key); return false; }
        return data.containsKey(key) || lists.containsKey(key)
            || sets.containsKey(key) || hashes.containsKey(key);
    }

    public boolean expire(String key, long seconds) {
        if (isExpired(key) || !exists(key)) { removeKey(key); return false; }
        if (seconds <= 0) { removeKey(key); return true; }
        long expiryTimestamp = System.currentTimeMillis() + (seconds * 1000L);
        expiryTimes.put(key, expiryTimestamp);
        return true;
    }

    public long ttl(String key) {
        if (isExpired(key)) { removeKey(key); return -2; }
        if (!exists(key)) return -2;
        Long expiryTimestamp = expiryTimes.get(key);
        if (expiryTimestamp == null) return -1;
        long remainingMillis = expiryTimestamp - System.currentTimeMillis();
        return Math.max(0, (remainingMillis + 999) / 1000);
    }

    // ============ LIST COMMANDS ============

    public int lpush(String key, String value) {
        lists.computeIfAbsent(key, k -> Collections.synchronizedList(new LinkedList<>()))
             .add(0, value);
        return lists.get(key).size();
    }

    public int rpush(String key, String value) {
        lists.computeIfAbsent(key, k -> Collections.synchronizedList(new LinkedList<>()))
             .add(value);
        return lists.get(key).size();
    }

    public List<String> lrange(String key, int start, int stop) {
        List<String> list = lists.get(key);
        if (list == null) return Collections.emptyList();
        synchronized (list) {
            int size = list.size();
            if (start < 0) start = Math.max(0, size + start);
            if (stop < 0) stop = size + stop;
            if (stop >= size) stop = size - 1;
            if (start > stop) return Collections.emptyList();
            return new ArrayList<>(list.subList(start, stop + 1));
        }
    }

    public int llen(String key) {
        List<String> list = lists.get(key);
        return list == null ? 0 : list.size();
    }

    // ============ SET COMMANDS ============

    public int sadd(String key, String member) {
        Set<String> set = sets.computeIfAbsent(key,
            k -> Collections.newSetFromMap(new ConcurrentHashMap<>()));
        return set.add(member) ? 1 : 0;
    }

    public Set<String> smembers(String key) {
        Set<String> set = sets.get(key);
        return set == null ? Collections.emptySet() : new HashSet<>(set);
    }

    public boolean sismember(String key, String member) {
        Set<String> set = sets.get(key);
        return set != null && set.contains(member);
    }

    public int scard(String key) {
        Set<String> set = sets.get(key);
        return set == null ? 0 : set.size();
    }

    // ============ HASH COMMANDS ============

    public int hset(String key, String field, String value) {
        Map<String, String> hash = hashes.computeIfAbsent(key,
            k -> new ConcurrentHashMap<>());
        boolean isNew = !hash.containsKey(field);
        hash.put(field, value);
        return isNew ? 1 : 0;
    }

    public String hget(String key, String field) {
        Map<String, String> hash = hashes.get(key);
        return hash == null ? null : hash.get(field);
    }

    public Map<String, String> hgetall(String key) {
        Map<String, String> hash = hashes.get(key);
        return hash == null ? Collections.emptyMap() : new HashMap<>(hash);
    }

    public int hlen(String key) {
        Map<String, String> hash = hashes.get(key);
        return hash == null ? 0 : hash.size();
    }

    // ============ INTERNAL ============

    private boolean isExpired(String key) {
        Long expiryTimestamp = expiryTimes.get(key);
        if (expiryTimestamp == null) return false;
        return System.currentTimeMillis() >= expiryTimestamp;
    }

    private void removeKey(String key) {
        data.remove(key);
        expiryTimes.remove(key);
        lists.remove(key);
        sets.remove(key);
        hashes.remove(key);
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

    public int size() { return data.size(); }

    public Map<String, String> getDataSnapshot() {
        return Collections.unmodifiableMap(data);
    }

    public Map<String, Long> getExpirySnapshot() {
        return Collections.unmodifiableMap(expiryTimes);
    }
}