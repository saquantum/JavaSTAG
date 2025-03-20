package edu.uob;

import java.io.File;
import java.nio.file.Paths;
import java.util.Scanner;

public class talkToServer {
    public static void main(String[] args){
        File entitiesFile = Paths.get("config" + File.separator + "basic-entities.dot").toAbsolutePath().toFile();
        File actionsFile = Paths.get("config" + File.separator + "basic-actions.xml").toAbsolutePath().toFile();
        GameServer server = new GameServer(entitiesFile, actionsFile);

        Scanner scanner = new Scanner(System.in);
        System.out.println("STAG Server running. Type commands below:");

        while (true) {
            System.out.print("> ");
            String input = scanner.nextLine().trim();
            if (input.equalsIgnoreCase("exit")) {
                System.out.println("Server shutting down...");
                break;
            }
            System.out.println(server.handleCommand(input));
        }

        scanner.close();
    }
}
