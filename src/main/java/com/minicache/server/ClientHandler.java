package com.minicache.server;

import com.minicache.command.CommandProcessor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private final Socket clientSocket;
    private final CommandProcessor processor;

    public ClientHandler(Socket clientSocket, CommandProcessor processor) {
        this.clientSocket = clientSocket;
        this.processor = processor;
    }

    @Override
    public void run() {
        String clientAddress = clientSocket.getRemoteSocketAddress().toString();
        System.out.println("[Server] Client connected: " + clientAddress);

        try (
            BufferedReader in = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            out.println("Connected to MiniCache server. Type commands (PING, SET, GET, DEL, EXISTS). Type EXIT to disconnect.");

            String line;
            while ((line = in.readLine()) != null) {
                if (line.trim().equalsIgnoreCase("EXIT") || line.trim().equalsIgnoreCase("QUIT")) {
                    out.println("Bye!");
                    break;
                }

                String response = processor.process(line);
                out.println(response);
            }

        } catch (IOException e) {
            System.out.println("[Server] Error handling client " + clientAddress + ": " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException ignored) {
            }
            System.out.println("[Server] Client disconnected: " + clientAddress);
        }
    }
}