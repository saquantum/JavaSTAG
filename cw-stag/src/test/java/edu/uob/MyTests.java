package edu.uob;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Paths;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

public class MyTests {

    private GameServer server;

    @BeforeEach
    void setup() {
        File entitiesFile = Paths.get("config" + File.separator + "my-entities.dot").toAbsolutePath().toFile();
        File actionsFile = Paths.get("config" + File.separator + "my-actions.xml").toAbsolutePath().toFile();
        server = new GameServer(entitiesFile, actionsFile);
    }

    String sendCommandToServer(String command) {
        return assertTimeoutPreemptively(Duration.ofMillis(5000), () -> {
                    return server.handleCommand(command);
                },
                "Server took too long to respond (probably stuck in an infinite loop)");
    }

    void assertRejectCommand(String command) {
        String response = sendCommandToServer(command).toLowerCase();
        assertTrue(response.contains("error") || response.contains("reject") || response.contains("not")
                || response.contains("can't") || response.contains("cant") || response.contains("don't")
                || response.contains("dont") || response.contains("unknown") || response.contains("recogni")
                || response.contains("invalid") || response.contains("refuse") || response.contains("unauthori")
                || response.contains("unreachable"));
    }

    // parsing entities:
    // check the first location.
    // create a storeroom if not included in the dot file.
    @Test
    void testParsingEntity(){
        String response = sendCommandToServer("simon: look");
        assertTrue(response.contains("cabin"));

        sendCommandToServer("simon: store key");
        response = sendCommandToServer("simon: look");
        assertFalse(response.contains("key"));

        sendCommandToServer("simon: retrieve with axe");
        response = sendCommandToServer("simon: look");
        assertTrue(response.contains("key"));
    }

    // parsing actions:
    // make sure the subjects, consumed and produced entities exists. (except 'health')

    // inv: print all items from current player. Check: All items must be in current player's inventory and no extra item.
    @Test
    void testInv(){
        sendCommandToServer("simon: get Axe;");
        sendCommandToServer("me: geT potion;");
        String response = sendCommandToServer("simon: check my inv!");
        assertTrue(response.contains("axe") && !response.contains("potion"));

        response = sendCommandToServer("me: inventory check");
        assertTrue(!response.contains("axe") && response.contains("potion"));
    }

    // get: picks up an artefact from current location. Check: Cannot get other types of entity. Cannot get artefact not at this location. Cannot get artefact owned by another player. Cannot get artefact non-existed.
    // drop: drop an artefact from current player. Check: The item is indeed an artefact. The artefact is in current player's inventory. The artefact is transferred to current location.
    // goto: goto a new location. Check: A direct path from current location to target location exists. Only current player is transferred to new location.
    // look: print all items at current location. Check: The location is the location where the specified player is at. Other players at this location are printed. All items should be included.
    // health: print current player's health. Check: Not health of another player is printed.

    // custom actions
    // check subjects are in the inventory of current player or at current location.
    // check consumed and produced items are not in another player's inventory.
    // check if location is consumed, only one way route is removed.
    // check if location is produced, only one way route is added.
    // write an action to consume or produce more than one health.

    // check: case insensitivity.
    // check decorative words.
    // check partial commands.
    // check word ordering.
    // check extra entity.
    // check ambiguous commands: write more than one 'open' action
    // check composite commands: 'get potion and axe', 'get potion and unlock trapdoor'

    // check player name
    // if player died, all items should be dropped and player transited to initial location.


    @Test
    void testLook() {

    }
}
