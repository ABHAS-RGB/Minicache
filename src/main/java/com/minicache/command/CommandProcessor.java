package com.minicache.command;

import com.minicache.store.KeyValueStore;

public class CommandProcessor {

    private final KeyValueStore store;

    public CommandProcessor(KeyValueStore store) {
        this.store = store;
    }

    public String process(String line) {
        if (line == null || line.trim().isEmpty()) {
            return "(error) empty command";
        }

        String[] parts = line.trim().split("\\s+", 3);
        String command = parts[0].toUpperCase();

        switch (command) {
            case "SET":
                return handleSet(parts);
            case "GET":
                return handleGet(parts);
            case "DEL":
                return handleDel(parts);
            case "EXISTS":
                return handleExists(parts);
            case "EXPIRE":
                return handleExpire(parts);
            case "TTL":
                return handleTtl(parts);
            case "PING":
                return "PONG";
            default:
                return "(error) unknown command '" + command + "'";
        }
    }

    private String handleSet(String[] parts) {
        if (parts.length < 3) {
            return "(error) wrong number of arguments for 'SET'. Usage: SET key value";
        }
        store.set(parts[1], parts[2]);
        return "OK";
    }

    private String handleGet(String[] parts) {
        if (parts.length < 2) {
            return "(error) wrong number of arguments for 'GET'. Usage: GET key";
        }
        String value = store.get(parts[1]);
        return (value == null) ? "(nil)" : value;
    }

    private String handleDel(String[] parts) {
        if (parts.length < 2) {
            return "(error) wrong number of arguments for 'DEL'. Usage: DEL key";
        }
        boolean removed = store.del(parts[1]);
        return removed ? "(integer) 1" : "(integer) 0";
    }

    private String handleExists(String[] parts) {
        if (parts.length < 2) {
            return "(error) wrong number of arguments for 'EXISTS'. Usage: EXISTS key";
        }
        boolean exists = store.exists(parts[1]);
        return exists ? "(integer) 1" : "(integer) 0";
    }

    private String handleExpire(String[] parts) {
        if (parts.length < 3) {
            return "(error) wrong number of arguments for 'EXPIRE'. Usage: EXPIRE key seconds";
        }

        long seconds;
        try {
            String secondsToken = parts[2].trim().split("\\s+")[0];
            seconds = Long.parseLong(secondsToken);
        } catch (NumberFormatException e) {
            return "(error) value is not an integer or out of range";
        }

        boolean set = store.expire(parts[1], seconds);
        return set ? "(integer) 1" : "(integer) 0";
    }

    private String handleTtl(String[] parts) {
        if (parts.length < 2) {
            return "(error) wrong number of arguments for 'TTL'. Usage: TTL key";
        }
        long ttl = store.ttl(parts[1]);
        return "(integer) " + ttl;
    }
}