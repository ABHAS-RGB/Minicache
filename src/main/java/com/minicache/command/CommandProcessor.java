package com.minicache.command;

import com.minicache.store.KeyValueStore;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class CommandProcessor {

    private final KeyValueStore store;

    public CommandProcessor(KeyValueStore store) {
        this.store = store;
    }

    public String process(String line) {
        if (line == null || line.trim().isEmpty()) {
            return "(error) empty command";
        }

        String[] parts = line.trim().split("\\s+", 4);
        String command = parts[0].toUpperCase();

        switch (command) {
            case "SET":     return handleSet(parts);
            case "GET":     return handleGet(parts);
            case "DEL":     return handleDel(parts);
            case "EXISTS":  return handleExists(parts);
            case "EXPIRE":  return handleExpire(parts);
            case "TTL":     return handleTtl(parts);
            case "LPUSH":   return handleLpush(parts);
            case "RPUSH":   return handleRpush(parts);
            case "LRANGE":  return handleLrange(parts);
            case "LLEN":    return handleLlen(parts);
            case "SADD":    return handleSadd(parts);
            case "SMEMBERS":return handleSmembers(parts);
            case "SISMEMBER":return handleSismember(parts);
            case "SCARD":   return handleScard(parts);
            case "HSET":    return handleHset(parts);
            case "HGET":    return handleHget(parts);
            case "HGETALL": return handleHgetall(parts);
            case "HLEN":    return handleHlen(parts);
            case "PING":    return "PONG";
            default:        return "(error) unknown command '" + command + "'";
        }
    }

    private String handleSet(String[] parts) {
        if (parts.length < 3) return "(error) Usage: SET key value";
        store.set(parts[1], parts[2]);
        return "OK";
    }

    private String handleGet(String[] parts) {
        if (parts.length < 2) return "(error) Usage: GET key";
        String value = store.get(parts[1]);
        return value == null ? "(nil)" : value;
    }

    private String handleDel(String[] parts) {
        if (parts.length < 2) return "(error) Usage: DEL key";
        return store.del(parts[1]) ? "(integer) 1" : "(integer) 0";
    }

    private String handleExists(String[] parts) {
        if (parts.length < 2) return "(error) Usage: EXISTS key";
        return store.exists(parts[1]) ? "(integer) 1" : "(integer) 0";
    }

    private String handleExpire(String[] parts) {
        if (parts.length < 3) return "(error) Usage: EXPIRE key seconds";
        try {
            long seconds = Long.parseLong(parts[2].trim().split("\\s+")[0]);
            return store.expire(parts[1], seconds) ? "(integer) 1" : "(integer) 0";
        } catch (NumberFormatException e) {
            return "(error) value is not an integer or out of range";
        }
    }

    private String handleTtl(String[] parts) {
        if (parts.length < 2) return "(error) Usage: TTL key";
        return "(integer) " + store.ttl(parts[1]);
    }

    // ============ LIST HANDLERS ============

    private String handleLpush(String[] parts) {
        if (parts.length < 3) return "(error) Usage: LPUSH key value";
        return "(integer) " + store.lpush(parts[1], parts[2]);
    }

    private String handleRpush(String[] parts) {
        if (parts.length < 3) return "(error) Usage: RPUSH key value";
        return "(integer) " + store.rpush(parts[1], parts[2]);
    }

    private String handleLrange(String[] parts) {
        if (parts.length < 4) return "(error) Usage: LRANGE key start stop";
        try {
            int start = Integer.parseInt(parts[2]);
            int stop = Integer.parseInt(parts[3]);
            List<String> result = store.lrange(parts[1], start, stop);
            if (result.isEmpty()) return "(empty list)";
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < result.size(); i++) {
                sb.append((i + 1)).append(") ").append(result.get(i));
                if (i < result.size() - 1) sb.append("\n");
            }
            return sb.toString();
        } catch (NumberFormatException e) {
            return "(error) value is not an integer or out of range";
        }
    }

    private String handleLlen(String[] parts) {
        if (parts.length < 2) return "(error) Usage: LLEN key";
        return "(integer) " + store.llen(parts[1]);
    }

    // ============ SET HANDLERS ============

    private String handleSadd(String[] parts) {
        if (parts.length < 3) return "(error) Usage: SADD key member";
        return "(integer) " + store.sadd(parts[1], parts[2]);
    }

    private String handleSmembers(String[] parts) {
        if (parts.length < 2) return "(error) Usage: SMEMBERS key";
        Set<String> members = store.smembers(parts[1]);
        if (members.isEmpty()) return "(empty set)";
        StringBuilder sb = new StringBuilder();
        int i = 1;
        for (String member : members) {
            sb.append(i++).append(") ").append(member);
            if (i <= members.size()) sb.append("\n");
        }
        return sb.toString();
    }

    private String handleSismember(String[] parts) {
        if (parts.length < 3) return "(error) Usage: SISMEMBER key member";
        return store.sismember(parts[1], parts[2]) ? "(integer) 1" : "(integer) 0";
    }

    private String handleScard(String[] parts) {
        if (parts.length < 2) return "(error) Usage: SCARD key";
        return "(integer) " + store.scard(parts[1]);
    }

    // ============ HASH HANDLERS ============

    private String handleHset(String[] parts) {
        if (parts.length < 4) return "(error) Usage: HSET key field value";
        return "(integer) " + store.hset(parts[1], parts[2], parts[3]);
    }

    private String handleHget(String[] parts) {
        if (parts.length < 3) return "(error) Usage: HGET key field";
        String value = store.hget(parts[1], parts[2]);
        return value == null ? "(nil)" : value;
    }

    private String handleHgetall(String[] parts) {
        if (parts.length < 2) return "(error) Usage: HGETALL key";
        Map<String, String> hash = store.hgetall(parts[1]);
        if (hash.isEmpty()) return "(empty hash)";
        StringBuilder sb = new StringBuilder();
        int i = 1;
        for (Map.Entry<String, String> entry : hash.entrySet()) {
            sb.append(i++).append(") ").append(entry.getKey()).append("\n");
            sb.append(i++).append(") ").append(entry.getValue()).append("\n");
        }
        return sb.toString().trim();
    }

    private String handleHlen(String[] parts) {
        if (parts.length < 2) return "(error) Usage: HLEN key";
        return "(integer) " + store.hlen(parts[1]);
    }
}