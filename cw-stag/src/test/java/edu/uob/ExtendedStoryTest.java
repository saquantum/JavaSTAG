package edu.uob;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Paths;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

public class ExtendedStoryTest {
    private GameServer server;

    @BeforeEach
    void setup() {
        File entitiesFile = Paths.get("config" + File.separator + "extended-entities.dot").toAbsolutePath().toFile();
        File actionsFile = Paths.get("config" + File.separator + "extended-actions.xml").toAbsolutePath().toFile();
        server = new GameServer(entitiesFile, actionsFile);
    }

    String sendCommandToServer(String command) {
        return assertTimeoutPreemptively(Duration.ofMillis(5000), () -> server.handleCommand(command));
    }

    void assertRejectCommand(String command) {
        String response = sendCommandToServer(command);
        assertTrue(response.contains("error") || response.contains("reject") || response.contains("not")
                || response.contains("can't") || response.contains("cant") || response.contains("don't")
                || response.contains("dont") || response.contains("unknown") || response.contains("recogni")
                || response.contains("invalid") || response.contains("refuse") || response.contains("unauthori")
                || response.contains("unreachable"));
    }

    @Test
    void testStory() {
        sendCommandToServer("sion: look");
        String response = sendCommandToServer("simon: look");
        assertTrue(response.contains("sion"));

        assertTrue(sendCommandToServer("simon: health").contains("3"));

        assertRejectCommand(sendCommandToServer("simon: get trapdoor"));
        assertRejectCommand(sendCommandToServer("simon: get key"));
        assertRejectCommand("simon: get");

        sendCommandToServer("simon: get potion and axe");
        response = sendCommandToServer("simon: inv");
        assertTrue(!response.contains("potion") && !response.contains("axe"));

        sendCommandToServer("simon: get potion");
        response = sendCommandToServer("simon: inv");
        assertTrue(response.contains("potion"));

        sendCommandToServer("simon: Get aXe");
        response = sendCommandToServer("simon: check inventory");
        assertTrue(response.contains("axe") && response.contains("potion"));

        response = sendCommandToServer("sion: look around");
        assertTrue(response.contains("simon") && response.contains("coin") && response.contains("trapdoor") && !response.contains("axe") && !response.contains("potion"));

        response = sendCommandToServer("simon: take a look");
        assertTrue(response.contains("sion") && response.contains("coin") && response.contains("trapdoor") && !response.contains("axe") && !response.contains("potion"));

        sendCommandToServer("simon: drop potion here");
        response = sendCommandToServer("sion: look");
        assertTrue(response.contains("potion"));
        sendCommandToServer("sion: potion I would like to get");

        sendCommandToServer("simon: goto Forest");
        sendCommandToServer("simon: get KEY");
        response = sendCommandToServer("simon: inV");
        assertTrue(response.contains("key"));

        // this should be valid since them all points to one action!!!!!!!!!!
        sendCommandToServer("simon: cut and cut down and chop the tree with axe");
        assertRejectCommand(sendCommandToServer("simon: cut down the tree"));
        sendCommandToServer("simon: get log");
        response = sendCommandToServer("simon: inv");
        assertTrue(response.contains("log"));

        sendCommandToServer("simon: goto the cabin");
        sendCommandToServer("sion: open the trapdoor with key I'm waiting here");
        response = sendCommandToServer("sion: look");
        assertFalse(response.contains("cellar"));

        sendCommandToServer("simon: open and unlock the trapdoor");
        assertRejectCommand(sendCommandToServer("simon: use my key to open the door"));
        response = sendCommandToServer("sion: look");
        assertTrue(response.contains("cellar"));

        sendCommandToServer("sion: let me goto cellar");
        sendCommandToServer("sion: hit elf");
        response = sendCommandToServer("sion: check health");
        assertTrue(response.contains("2"));
        sendCommandToServer("sion: attack elf");
        response = sendCommandToServer("sion: health check");
        assertTrue(response.contains("1"));
        sendCommandToServer("sion: hit elf");
        response = sendCommandToServer("sion: check health");
        assertTrue(response.contains("3"));
        sendCommandToServer("sion: fight with elf");
        response = sendCommandToServer("sion: check health");
        assertTrue(response.contains("3"));
        response = sendCommandToServer("sion: look");
        assertTrue(response.contains("cabin"));
        response = sendCommandToServer("sion: inv");
        assertFalse(response.contains("potion"));

        sendCommandToServer("sion: goto cellar again!");
        sendCommandToServer("sion: elf hit");
        sendCommandToServer("sion: hit elf");
        response = sendCommandToServer("sion: health");
        assertTrue(response.contains("1"));
        sendCommandToServer("sion: get that potion");
        sendCommandToServer("sion: drink simon's potion");
        response = sendCommandToServer("sion: health");
        assertTrue(response.contains("2"));
        assertRejectCommand("sion: get elf");

        sendCommandToServer("simon: get coin");
        sendCommandToServer("simon: goto cellar");
        sendCommandToServer("simon: pay elf");
        sendCommandToServer("sion: get shovel");
        response = sendCommandToServer("sion: inv");
        assertTrue(response.contains("shovel"));
        response = sendCommandToServer("simon: inv");
        assertFalse(response.contains("shovel"));

        sendCommandToServer("simon: goto cabin");
        sendCommandToServer("simon: goto forest");
        sendCommandToServer("simon: goto riverbank");
        assertRejectCommand(sendCommandToServer("simon: goto clearing"));
        sendCommandToServer("simon: log bridge");
        sendCommandToServer("simon: goto clearing");

        sendCommandToServer("sion: goto cabin");
        sendCommandToServer("sion: goto forest");
        sendCommandToServer("sion: goto riverbank");
        sendCommandToServer("sion: get horn");
        sendCommandToServer("sion: goto clearing");

        sendCommandToServer("sion: to dig the ground is what we want");
        response = sendCommandToServer("simon: look");
        assertTrue(response.contains("gold"));

        sendCommandToServer("sion: blow the Gjallar's horn to invoke the Ragnarok !");
        response = sendCommandToServer("simon: look");
        assertTrue(response.contains("lumberjack"));
    }
}
