package com.minicache.server;

import com.minicache.command.CommandProcessor;
import com.minicache.store.KeyValueStore;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Server is the "front door" of MiniCache.
 *
 * It does ONE job in a loop:
 *   1. Wait for a client to connect (accept())
 *   2. Hand that client off to a new ClientHandler running on its own Thread
 *   3. Immediately go back to waiting for the NEXT client
 *
 * This is the "thread-per-connection" model:
 * - Pros: simple to understand and implement, each client's logic is
 *   isolated (one client's slow command doesn't block others).
 * - Cons: doesn't scale to huge numbers of connections (e.g. 10,000+)
 *   because each thread consumes OS memory (~1MB stack by default) and
 *   the OS has to context-switch between them. Production systems
 *   (real Redis, Netty-based servers) use event loops / async I/O
 *   to avoid this. Good to mention this tradeoff in interviews.
 */
public class Server {

    private final int port;
    private final KeyValueStore store;

    public Server(int port) {
        this.port = port;
        this.store = new KeyValueStore();
    }

    public void start() {
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
}