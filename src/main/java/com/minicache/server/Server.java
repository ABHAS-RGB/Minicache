package com.minicache.server;

import com.minicache.command.CommandProcessor;
import com.minicache.store.KeyValueStore;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    private final int port;
    private final KeyValueStore store;

    private static final long CLEANUP_INTERVAL_MS = 1000;

    public Server(int port) {
        this.port = port;
        this.store = new KeyValueStore();
    }

    public void start() {
        startExpirationThread();

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
}