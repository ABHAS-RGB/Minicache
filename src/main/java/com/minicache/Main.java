package com.minicache;

import com.minicache.command.CommandProcessor;
import com.minicache.store.KeyValueStore;

import java.util.Scanner;

/**
 * Main entry point for MiniCache - Phase 1.
 *
 * This is a CLI (command-line) REPL: it reads a command, evaluates it
 * against the store, prints the result, and loops.
 *
 * In Phase 2, we'll add a TCP server that does almost the exact same
 * thing (read line -> process -> respond) but over a socket instead
 * of System.in/System.out. The CommandProcessor we wrote will be
 * reused as-is.
 */
public class Main {

    public static void main(String[] args) {
        KeyValueStore store = new KeyValueStore();
        CommandProcessor processor = new CommandProcessor(store);

        System.out.println("MiniCache v0.1 (Phase 1 - in-memory store, CLI mode)");
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