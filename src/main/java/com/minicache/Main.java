package com.minicache;

import com.minicache.command.CommandProcessor;
import com.minicache.server.Server;
import com.minicache.store.KeyValueStore;

import java.util.Scanner;

public class Main {

    private static final int DEFAULT_PORT = 6380;

    public static void main(String[] args) {
        String mode = (args.length > 0) ? args[0].toLowerCase() : "cli";

        switch (mode) {
            case "server":
                int port = DEFAULT_PORT;
                if (args.length > 1) {
                    try {
                        port = Integer.parseInt(args[1]);
                    } catch (NumberFormatException e) {
                        System.err.println("Invalid port '" + args[1] + "', using default " + DEFAULT_PORT);
                    }
                }
                new Server(port).start();
                break;

            case "cli":
            default:
                runCli();
                break;
        }
    }

    private static void runCli() {
        KeyValueStore store = new KeyValueStore();
        CommandProcessor processor = new CommandProcessor(store);

        System.out.println("MiniCache v0.2 (CLI mode)");
        System.out.println("Supported commands: SET key value | GET key | DEL key | EXISTS key | PING | EXIT");
        System.out.println();

        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("minicache> ");
            if (!scanner.hasNextLine()) {
                break;
            }
            String line = scanner.nextLine();

            if (line.trim().equalsIgnoreCase("EXIT") || line.trim().equalsIgnoreCase("QUIT")) {
                System.out.println("Bye!");
                break;
            }

            String response = processor.process(line);
            System.out.println(response);
        }

        scanner.close();
    }
}