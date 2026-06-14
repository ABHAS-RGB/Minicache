package com.minicache.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.minicache.store.KeyValueStore;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;

/**
 * PersistenceManager handles saving and loading the store to/from disk.
 *
 * Save format: JSON file with two maps:
 *   {
 *     "data": { "key1": "value1", "key2": "value2" },
 *     "expiryTimes": { "key2": 1718123456789 }
 *   }
 *
 * Atomic writes: we write to a temp file first, then rename it to the
 * real file. Renaming is atomic on most OS/filesystems - if the process
 * crashes mid-write, the old file is still intact (not half-written).
 * This is a critical pattern in any system that writes important data.
 *
 * Why Gson?
 * - Java has no built-in JSON library. Gson is Google's library,
 *   widely used, and one line to add via Maven. Shows you know how
 *   to manage dependencies in real projects.
 */
public class PersistenceManager {

    private static final String SAVE_FILE = "dump.json";
    private static final String TEMP_FILE = "dump.json.tmp";

    private final KeyValueStore store;
    private final Gson gson;

    public PersistenceManager(KeyValueStore store) {
        this.store = store;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    /**
     * Saves current store state to disk atomically.
     * Returns true if save succeeded, false if it failed.
     */
    public boolean save() {
        try {
            // Get a snapshot of both maps from the store
            Map<String, String> data = store.getDataSnapshot();
            Map<String, Long> expiryTimes = store.getExpirySnapshot();

            // Build a simple wrapper object to serialize both maps together
            SaveData saveData = new SaveData(data, expiryTimes);
            String json = gson.toJson(saveData);

            // Write to temp file first (atomic write pattern)
            Path tempPath = Paths.get(TEMP_FILE);
            Path savePath = Paths.get(SAVE_FILE);

            try (Writer writer = new FileWriter(tempPath.toFile())) {
                writer.write(json);
            }

            // Atomically replace the real file with the temp file.
            // If this process was killed during the write above,
            // dump.json is still intact from the previous save.
            Files.move(tempPath, savePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

            System.out.println("[Persistence] Saved " + data.size() + " key(s) to " + SAVE_FILE);
            return true;

        } catch (IOException e) {
            System.err.println("[Persistence] Save failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Loads store state from disk on startup.
     * Returns the number of keys loaded, or -1 if load failed.
     * Returns 0 silently if no save file exists (fresh start).
     */
    public int load() {
        Path savePath = Paths.get(SAVE_FILE);

        if (!Files.exists(savePath)) {
            System.out.println("[Persistence] No save file found, starting fresh.");
            return 0;
        }

        try {
            String json = new String(Files.readAllBytes(savePath));

            Type type = new TypeToken<SaveData>(){}.getType();
            SaveData saveData = gson.fromJson(json, type);

            if (saveData == null || saveData.data == null) {
                System.out.println("[Persistence] Save file is empty or corrupt, starting fresh.");
                return 0;
            }

            // Filter out already-expired keys before loading them back.
            // No point restoring a key whose TTL expired while server was down.
            long now = System.currentTimeMillis();
            int loaded = 0;
            int skipped = 0;

            for (Map.Entry<String, String> entry : saveData.data.entrySet()) {
                String key = entry.getKey();
                Long expiryTimestamp = saveData.expiryTimes != null
                    ? saveData.expiryTimes.get(key)
                    : null;

                // Skip keys that expired while the server was offline
                if (expiryTimestamp != null && now >= expiryTimestamp) {
                    skipped++;
                    continue;
                }

                // Restore the key and its TTL (if any)
                store.set(key, entry.getValue());
                if (expiryTimestamp != null) {
                    long remainingSeconds = (expiryTimestamp - now) / 1000;
                    store.expire(key, remainingSeconds);
                }
                loaded++;
            }

            System.out.println("[Persistence] Loaded " + loaded + " key(s) from " + SAVE_FILE +
                (skipped > 0 ? " (skipped " + skipped + " expired key(s))" : ""));
            return loaded;

        } catch (IOException e) {
            System.err.println("[Persistence] Load failed: " + e.getMessage());
            return -1;
        }
    }

    /**
     * Simple wrapper class for Gson serialization.
     * Gson serializes all fields automatically - no annotations needed.
     */
    private static class SaveData {
        Map<String, String> data;
        Map<String, Long> expiryTimes;

        SaveData(Map<String, String> data, Map<String, Long> expiryTimes) {
            this.data = data;
            this.expiryTimes = expiryTimes;
        }
    }
}