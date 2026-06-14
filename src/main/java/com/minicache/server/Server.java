package com.minicache.server;

import com.minicache.command.CommandProcessor;
import com.minicache.persistence.PersistenceManager;
import com.minicache.store.KeyValueStore;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    private final int port;
    private final KeyValueStore store;
    private final PersistenceManager persistenceManager;

    private static final long CLEANUP_INTERVAL_MS = 1000;
    private static final long SAVE_INTERVAL_MS = 60000; // save every 60 seconds

    public Server(int port) {
        this.port = port;
        this.store = new KeyValueStore();
        this.persistenceManager = new PersistenceManager(store);
    }

    public void start() {
        // Load saved data from disk before accepting any connections
        persistenceManager.load();

        startExpirationThread();
        startPersistenceThread();
        registerShutdownHook();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("MiniCache server listening on port " + port);
            System.out.println("Connect using: telnet localhost " + port);
            System.out.println("(or PowerShell: Test-NetConnection -ComputerName localhost -Port " + port + ")");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                CommandProcessor processor = new CommandProcessor(store);
                Thread clientThread = new Thread(new ClientHandler(clientSocket, processor));
                clientThread.start();
            }
        } catch (IOException e) {
            System.err.println("[Server] Failed to start on port " + port + ": " + e.getMessage());
        }
    }

    private void startExpirationThread() {
        Thread expirationThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(CLEANUP_INTERVAL_MS);
                    int removed = store.removeExpiredKeys();
                    if (removed > 0) {
                        System.out.println("[Expiration] Removed " + removed + " expired key(s)");
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        expirationThread.setDaemon(true);
        expirationThread.setName("expiration-cleanup");
        expirationThread.start();
        System.out.println("[Server] Background expiration thread started (interval: " + CLEANUP_INTERVAL_MS + "ms)");
    }

    /**
     * Background thread that saves the store to disk every SAVE_INTERVAL_MS.
     * This is the periodic snapshot - like Redis "save 60 1" config.
     * Even if the server crashes between saves, we only lose at most
     * SAVE_INTERVAL_MS worth of writes (60 seconds by default).
     */
    private void startPersistenceThread() {
        Thread persistenceThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(SAVE_INTERVAL_MS);
                    persistenceManager.save();
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        persistenceThread.setDaemon(true);
        persistenceThread.setName("persistence-snapshot");
        persistenceThread.start();
        System.out.println("[Server] Background persistence thread started (interval: " + SAVE_INTERVAL_MS + "ms)");
    }

    /**
     * Registers a JVM shutdown hook - a thread that runs automatically
     * when the JVM exits (Ctrl+C, SIGTERM, or System.exit()).
     *
     * This ensures we do a FINAL save on clean shutdown, so we never
     * lose data that was written since the last periodic snapshot.
     *
     * Interview talking point: "I implemented a shutdown hook to
     * guarantee a final flush to disk on graceful shutdown, reducing
     * data loss to zero for normal restarts."
     */
    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[Server] Shutdown signal received, saving data...");
            persistenceManager.save();
            System.out.println("[Server] Shutdown complete.");
        }));
        System.out.println("[Server] Shutdown hook registered (data will be saved on exit).");
    }
}