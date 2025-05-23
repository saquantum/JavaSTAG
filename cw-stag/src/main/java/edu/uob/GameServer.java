package edu.uob;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Paths;

public final class GameServer {

    private static final char END_OF_TRANSMISSION = 4;

    private Controller controller = null;

    public static void main(String[] args) throws IOException {
        File entitiesFile = Paths.get(new StringBuilder().append("config").append(File.separator).append("basic-entities.dot").toString()).toAbsolutePath().toFile();
        File actionsFile = Paths.get(new StringBuilder().append("config").append(File.separator).append("basic-actions.xml").toString()).toAbsolutePath().toFile();
        GameServer server = new GameServer(entitiesFile, actionsFile);
        server.blockingListenOn(8888);
    }

    /**
    * Do not change the following method signature or we won't be able to mark your submission
    * Instanciates a new server instance, specifying a game with some configuration files
    *
    * @param entitiesFile The game configuration file containing all game entities to use in your game
    * @param actionsFile The game configuration file containing all game actions to use in your game
    */
    public GameServer(File entitiesFile, File actionsFile) {
        try {
            this.controller = new Controller(entitiesFile, actionsFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
    * Do not change the following method signature or we won't be able to mark your submission
    * This method handles all incoming game commands and carries out the corresponding actions.</p>
    *
    * @param command The incoming command to be processed
    */
    public String handleCommand(String command) {
        if(this.controller == null) return "[ERROR]: Invalid building files so server has no valid controlling unit!";
        try {
            return this.controller.handleCommand(command);
        } catch (MyExceptions e) {
            return e.getMessage();
        }
    }

    /**
    * Do not change the following method signature or we won't be able to mark your submission
    * Starts a *blocking* socket server listening for new connections.
    *
    * @param portNumber The port to listen on.
    * @throws IOException If any IO related operation fails.
    */
    public void blockingListenOn(int portNumber) throws IOException {
        try (ServerSocket s = new ServerSocket(portNumber)) {
            System.out.println(new StringBuilder().append("Server listening on port ").append(portNumber));
            while (!Thread.interrupted()) {
                try {
                    this.blockingHandleConnection(s);
                } catch (IOException e) {
                    System.out.println("Connection closed");
                }
            }
        }
    }

    /**
    * Do not change the following method signature or we won't be able to mark your submission
    * Handles an incoming connection from the socket server.
    *
    * @param serverSocket The client socket to read/write from.
    * @throws IOException If any IO related operation fails.
    */
    private void blockingHandleConnection(ServerSocket serverSocket) throws IOException {
        try (Socket s = serverSocket.accept();
        BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()))) {
            System.out.println("Connection established");
            String incomingCommand = reader.readLine();
            if(incomingCommand != null) {
                System.out.println(new StringBuilder().append("Received message from ").append(incomingCommand));
                String result = this.handleCommand(incomingCommand);
                writer.write(result);
                writer.write(new StringBuilder().append("\n").append(END_OF_TRANSMISSION).append("\n").toString());
                writer.flush();
            }
        }
    }
}
