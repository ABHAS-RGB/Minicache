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

public class PersistenceManager {

    private final String saveFile;
    private final String tempFile;
    private final KeyValueStore store;
    private final Gson gson;

    public PersistenceManager(KeyValueStore store) {
        this.store = store;
        this.gson = new GsonBuilder().setPrettyPrinting().create();

        // Respect SAVE_DIR env variable (set in Dockerfile to /data).
        // Falls back to current directory if not set (local development).
        String saveDir = System.getenv("SAVE_DIR");
        if (saveDir != null && !saveDir.isEmpty()) {
            this.saveFile = saveDir + "/dump.json";
            this.tempFile = saveDir + "/dump.json.tmp";
        } else {
            this.saveFile = "dump.json";
            this.tempFile = "dump.json.tmp";
        }

        System.out.println("[Persistence] Save location: " + this.saveFile);
    }

    public boolean save() {
        try {
            Map<String, String> data = store.getDataSnapshot();
            Map<String, Long> expiryTimes = store.getExpirySnapshot();

            SaveData saveData = new SaveData(data, expiryTimes);
            String json = gson.toJson(saveData);

            Path tempPath = Paths.get(tempFile);
            Path savePath = Paths.get(saveFile);

            try (Writer writer = new FileWriter(tempPath.toFile())) {
                writer.write(json);
            }

            Files.move(tempPath, savePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

            System.out.println("[Persistence] Saved " + data.size() + " key(s) to " + saveFile);
            return true;

        } catch (IOException e) {
            System.err.println("[Persistence] Save failed: " + e.getMessage());
            return false;
        }
    }

    public int load() {
        Path savePath = Paths.get(saveFile);

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

            long now = System.currentTimeMillis();
            int loaded = 0;
            int skipped = 0;

            for (Map.Entry<String, String> entry : saveData.data.entrySet()) {
                String key = entry.getKey();
                Long expiryTimestamp = saveData.expiryTimes != null
                    ? saveData.expiryTimes.get(key)
                    : null;

                if (expiryTimestamp != null && now >= expiryTimestamp) {
                    skipped++;
                    continue;
                }

                store.set(key, entry.getValue());
                if (expiryTimestamp != null) {
                    long remainingSeconds = (expiryTimestamp - now) / 1000;
                    store.expire(key, remainingSeconds);
                }
                loaded++;
            }

            System.out.println("[Persistence] Loaded " + loaded + " key(s) from " + saveFile +
                (skipped > 0 ? " (skipped " + skipped + " expired key(s))" : ""));
            return loaded;

        } catch (IOException e) {
            System.err.println("[Persistence] Load failed: " + e.getMessage());
            return -1;
        }
    }

    private static class SaveData {
        Map<String, String> data;
        Map<String, Long> expiryTimes;

        SaveData(Map<String, String> data, Map<String, Long> expiryTimes) {
            this.data = data;
            this.expiryTimes = expiryTimes;
        }
    }
}