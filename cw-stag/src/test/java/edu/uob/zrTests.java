package edu.uob;

import com.alexmerz.graphviz.ParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class zrTests {

    private GameServer server;

    // Create a new server _before_ every @Test
    @BeforeEach
    void setup() throws ParserConfigurationException, IOException, ParseException, SAXException {
        File entitiesFile = Paths.get("config" + File.separator + "zr-entities.dot").toAbsolutePath().toFile();
        File actionsFile = Paths.get("config" + File.separator + "extended-actions.xml").toAbsolutePath().toFile();
        server = new GameServer(entitiesFile, actionsFile);
    }

    String sendCommandToServer(String command) {
        // Try to send a command to the server - this call will timeout if it takes too long (in case the server enters an infinite loop)
        return server.handleCommand(command);
    }

    void assertRejectCommand(String command) {
        String response = sendCommandToServer(command).toLowerCase();
        assertTrue(response.contains("error") || response.contains("reject") || response.contains("not")
                || response.contains("can't") || response.contains("cant") || response.contains("don't")
                || response.contains("dont") || response.contains("unknown") || response.contains("recogni")
                || response.contains("invalid") || response.contains("refuse") || response.contains("unauthori")
                || response.contains("unreachable"));
    }

    // A lot of tests will probably check the game state using 'look' - so we better make sure 'look' works well !
    @Test
    void testLook() {
        String response = sendCommandToServer("simon: look");
        response = response.toLowerCase();
        assertTrue(response.contains("cabin"), "Did not see the name of the current room in response to look");
        assertTrue(response.contains("log cabin"), "Did not see a description of the room in response to look");
        assertTrue(response.contains("magic potion"), "Did not see a description of artifacts in response to look");
        assertTrue(response.contains("wooden trapdoor"), "Did not see description of furniture in response to look");
        assertTrue(response.contains("forest"), "Did not see available paths in response to look");
    }

    // Test that we can pick something up and that it appears in our inventory
    @Test
    void testGet() {
        String response;
        sendCommandToServer("simon: get potion");
        response = sendCommandToServer("simon: inv");
        response = response.toLowerCase();
        assertTrue(response.contains("potion"), "Did not see the potion in the inventory after an attempt was made to get it");
        response = sendCommandToServer("simon: look");
        response = response.toLowerCase();
        assertFalse(response.contains("potion"), "Potion is still present in the room after an attempt was made to get it");
    }

    // Test that we can goto a different location (we won't get very far if we can't move around the game !)
    @Test
    void testGoto() {
        sendCommandToServer("simon: goto forest");
        String response = sendCommandToServer("simon: look");
        response = response.toLowerCase();
        assertTrue(response.contains("key"), "Failed attempt to use 'goto' command to move to the forest - there is no key in the current location");
    }

    // Add more unit tests or integration tests here.
    @Test
    void testCaseInsensitive() {
        String response;
        response = sendCommandToServer("simon: look").toLowerCase();
        assertTrue(response.contains("cabin"), "Did not see the name of the current room in response to look");
        assertTrue(response.contains("log cabin"), "Did not see a description of the room in response to look");
        assertTrue(response.contains("magic potion"), "Did not see a description of artifacts in response to look");
        assertTrue(response.contains("wooden trapdoor"), "Did not see description of furniture in response to look");
        assertTrue(response.contains("forest"), "Did not see available paths in response to look");

        response = sendCommandToServer("simon: LooK").toLowerCase();
        assertTrue(response.contains("cabin"), "Did not see the name of the current room in response to look");
        assertTrue(response.contains("log cabin"), "Did not see a description of the room in response to look");
        assertTrue(response.contains("magic potion"), "Did not see a description of artifacts in response to look");
        assertTrue(response.contains("wooden trapdoor"), "Did not see description of furniture in response to look");
        assertTrue(response.contains("forest"), "Did not see available paths in response to look");
    }

    @Test
    void testStupidCommand(){
        String response;
        response = sendCommandToServer("ajsdnas  89y23ghas");
        assertRejectCommand(response);
        response = sendCommandToServer(")(^656hh");
        assertRejectCommand(response);
        response = sendCommandToServer("look look");
    }

    @Test
    void testInvalidBuildInCommand(){
        String response;
        response = sendCommandToServer("Please look around");
        response = sendCommandToServer("look cabin");
        assertFalse(response.contains("cabin"));
        sendCommandToServer("get axe");
        response = sendCommandToServer("inv axe");
        assertFalse(response.contains("axe"));
        response = sendCommandToServer("health axe");
        assertFalse(response.contains("3"));
    }

    @Test
    void testBasicAction(){
        String response;
        sendCommandToServer("get axe");
        response = sendCommandToServer("inv").toLowerCase();
        assertTrue(response.contains("axe"));
        sendCommandToServer("goto forest");
        sendCommandToServer("get key");
        sendCommandToServer("cut tree with axe");
        response = sendCommandToServer("look");
        assertTrue(response.contains("log"));
        sendCommandToServer("goto cabin");
        System.out.println(sendCommandToServer("inv"));
        sendCommandToServer("open the trapdoor with key");
        response = sendCommandToServer("inventory");
        System.out.println(response);
        assertFalse(response.contains("key"), "Fail to consume");
        response = sendCommandToServer("look");
        assertFalse(response.contains("key"), "Fail to consume");
        response = sendCommandToServer("storeroom");
    }

    @Test
    void testExtendedAction(){
        String response;
        sendCommandToServer("get axe");
        sendCommandToServer("get potion");
        sendCommandToServer("get coin");
        response = sendCommandToServer("inv").toLowerCase();
        assertTrue(response.contains("axe"));
        sendCommandToServer("goto forest");
        sendCommandToServer("get key");
        sendCommandToServer("cut down tree with axe");
        response = sendCommandToServer("look");
        assertTrue(response.contains("log"));
        sendCommandToServer("get log");
        sendCommandToServer("goto cabin");
        sendCommandToServer("inv");
        sendCommandToServer("open the trapdoor with key");
        response = sendCommandToServer("inventory");
        assertFalse(response.contains("key"), "Fail to consume");
        response = sendCommandToServer("look");
        assertFalse(response.contains("key"), "Fail to consume");
        sendCommandToServer("goto cellar");
        sendCommandToServer("fight with the elf");
        response = sendCommandToServer("health");
        assertTrue(response.contains("2"));
        response = sendCommandToServer("drink potion");
        assertTrue(response.contains("You drink the potion and your health improves"));
        response = sendCommandToServer("health");
        assertTrue(response.contains("3"));
        sendCommandToServer("pay the elf with coin");
        response = sendCommandToServer("look");
        assertTrue(response.contains("shovel"));
        sendCommandToServer("get shovel");
        response = sendCommandToServer("inventory");
        assertFalse(response.contains("coin"));
        sendCommandToServer("goto cabin");
        sendCommandToServer("goto forest");
        sendCommandToServer("goto riverbank");
        sendCommandToServer("look");
        sendCommandToServer("bridge the river with log");
        sendCommandToServer("goto clearing");
        response = sendCommandToServer("look");
        assertTrue(response.contains("clearing"));
         sendCommandToServer("dig the ground with shovel");
        response = sendCommandToServer("look");
        assertFalse(response.contains("It looks like the soil has been recently disturbed"));
        assertTrue(response.contains("hole"));
        assertTrue(response.contains("gold"));
        sendCommandToServer("get gold");
        response = sendCommandToServer("inv");
        assertTrue(response.contains("gold"));
    }

    @Test
    void testInvalidPath(){
        String response;
        sendCommandToServer("goto cellar");
        response = sendCommandToServer("look");
        assertFalse(response.contains("cellar"));
    }

    @Test
    void testSeeingOtherPlayers(){
        String response;
        sendCommandToServer("Simon : look");
        response = sendCommandToServer("sion : look");
        assertTrue(response.contains("Simon"));
    }

    @Test
    void testTwoWayPath(){
        String response;
        sendCommandToServer("goto forest");
        response = sendCommandToServer("look");
        assertTrue(response.contains("cabin"));
        assertTrue(response.contains("riverbank"));
    }

    @Test
    void testConsumePath(){
        assertTrue(sendCommandToServer("look").contains("forest"));
        System.out.println(sendCommandToServer("please destroy path with axe"));
        assertFalse(sendCommandToServer("look").contains("forest"));
    }

    @Test
    void testPartialCommand(){
        String response;
        // sendCommandToServer("get axe");
        sendCommandToServer("goto forest");
        sendCommandToServer("get key");
        sendCommandToServer("goto cabin");
        sendCommandToServer("unlock with key");
        sendCommandToServer("goto cellar");
        response = sendCommandToServer("look");
        assertTrue(response.contains("cellar"));
    }

    @Test
    void testExtraneousEntities(){
        String response;
        sendCommandToServer("goto forest");
        sendCommandToServer("get key");
        sendCommandToServer("goto cabin");
        sendCommandToServer("open the trapdoor with key and potion");
        sendCommandToServer("goto cellar");
        response = sendCommandToServer("look");
        assertFalse(response.contains("cellar"));
    }

    @Test
    void testAmbiguousCommands(){
        String response;
        sendCommandToServer("get axe");
        sendCommandToServer("goto forest");
        sendCommandToServer("get key");
        sendCommandToServer("goto cabin");
        sendCommandToServer("unlock with key");
        sendCommandToServer("look");
        sendCommandToServer("destroy with coin");
        sendCommandToServer("goto forest");
        response = sendCommandToServer("look");
        assertTrue(response.contains("forest"));
        sendCommandToServer("goto cabin");
        sendCommandToServer("destroy path with coin and axe");
        sendCommandToServer("goto forest");
        response = sendCommandToServer("look");
        assertFalse(response.contains("forest"));
    }

    @Test
    void testHealthAndRespawn(){
        String response;
        sendCommandToServer("get potion");
        sendCommandToServer("use potion heal player");
        response = sendCommandToServer("health");
        assertTrue(response.contains("3"));
        sendCommandToServer("goto forest");
        sendCommandToServer("use potion to attack player");
        response = sendCommandToServer("health");
        assertTrue(response.contains("2"));
        sendCommandToServer("use potion attack player");
        sendCommandToServer("use potion attack player");
        response = sendCommandToServer("look");
        assertTrue(response.contains("cabin"));
        response = sendCommandToServer("inv");
        assertFalse(response.contains("potion"));
        sendCommandToServer("goto forest");
        response = sendCommandToServer("look");
        assertTrue(response.contains("potion"));
    }

    @Test
    void testSeparateKeyphrase(){
        String response;
        sendCommandToServer("attack the player");
        response = sendCommandToServer("health");
        assertTrue(response.contains("3"));
    }

    @Test
    void testDuplicateKeyphrase(){
        String response;
        sendCommandToServer("goto forest");
        sendCommandToServer("get key");
        sendCommandToServer("goto cabin");
        sendCommandToServer("open and unlock the trapdoor with key");
        response = sendCommandToServer("inv");
        assertFalse(response.contains("key"));
    }
}
