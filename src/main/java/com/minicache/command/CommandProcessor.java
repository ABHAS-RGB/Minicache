package com.minicache.command;

import com.minicache.store.KeyValueStore;

/**
 * CommandProcessor takes a raw command line (e.g. "SET name John")
 * and executes it against the KeyValueStore, returning a response string.
 *
 * Why separate this from KeyValueStore and from Main?
 * - KeyValueStore only knows about data (get/set/del) — it has no idea
 *   what a "command line" looks like.
 * - Main (or later, the network server) just needs to pass in a line
 *   of text and get a line of text back. This means Phase 2's TCP
 *   server can reuse this class UNCHANGED, just swapping CLI input
 *   for socket input.
 */
public class CommandProcessor {

    private final KeyValueStore store;

    public CommandProcessor(KeyValueStore store) {
        this.store = store;
    }

    public String process(String line) {
        if (line == null || line.trim().isEmpty()) {
            return "(error) empty command";
        }

        // Split on whitespace. limit=3 means SET only splits into 3 parts max,
        // so values containing spaces (e.g. "SET msg hello world") are preserved
        // as one value: ["SET", "msg", "hello world"]
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
}