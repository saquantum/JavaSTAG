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
        File entitiesFile = Paths.get("config" + File.separator + "extended-entities.dot").toAbsolutePath().toFile();
        File actionsFile = Paths.get("config" + File.separator + "zr-actions.xml").toAbsolutePath().toFile();
        server = new GameServer(entitiesFile, actionsFile);
    }

    String sendCommandToServer(String command) {
        // Try to send a command to the server - this call will timeout if it takes too long (in case the server enters an infinite loop)
        return server.handleCommand(command);
    }

    void assertRejectCommand(String response) {
        response = response.toLowerCase();
        assertTrue(response.contains("error") || response.contains("reject") || response.contains("cannot")
                || response.contains("can't") || response.contains("cant") || response.contains("don't")
                || response.contains("dont") || response.contains("unknown") || response.contains("recogni")
                || response.contains("invalid") || response.contains("refuse") || response.contains("unauthori")
                || response.contains("unreachable") || response.contains("do not"));
    }

    // A lot of tests will probably check the game state using 'look' - so we better make sure 'look' works well !
    @Test
    void testLook() {
        String response = sendCommandToServer("username: simon: look");
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
        sendCommandToServer("username: simon: get potion");
        response = sendCommandToServer("username: simon: inv");
        response = response.toLowerCase();
        assertTrue(response.contains("potion"), "Did not see the potion in the inventory after an attempt was made to get it");
        response = sendCommandToServer("username: simon: look");
        response = response.toLowerCase();
        assertFalse(response.contains("potion"), "Potion is still present in the room after an attempt was made to get it");
    }

    // Test that we can goto a different location (we won't get very far if we can't move around the game !)
    @Test
    void testGoto() {
        sendCommandToServer("username: simon: goto forest");
        String response = sendCommandToServer("username: simon: look");
        response = response.toLowerCase();
        assertTrue(response.contains("key"), "Failed attempt to use 'goto' command to move to the forest - there is no key in the current location");
    }

    // Add more unit tests or integration tests here.
    @Test
    void testCaseInsensitive() {
        String response;
        response = sendCommandToServer("username: simon: look").toLowerCase();
        assertTrue(response.contains("cabin"), "Did not see the name of the current room in response to look");
        assertTrue(response.contains("log cabin"), "Did not see a description of the room in response to look");
        assertTrue(response.contains("magic potion"), "Did not see a description of artifacts in response to look");
        assertTrue(response.contains("wooden trapdoor"), "Did not see description of furniture in response to look");
        assertTrue(response.contains("forest"), "Did not see available paths in response to look");

        response = sendCommandToServer("username: simon: LooK").toLowerCase();
        assertTrue(response.contains("cabin"), "Did not see the name of the current room in response to look");
        assertTrue(response.contains("log cabin"), "Did not see a description of the room in response to look");
        assertTrue(response.contains("magic potion"), "Did not see a description of artifacts in response to look");
        assertTrue(response.contains("wooden trapdoor"), "Did not see description of furniture in response to look");
        assertTrue(response.contains("forest"), "Did not see available paths in response to look");
    }

    @Test
    void testStupidCommand(){
        String response;
        response = sendCommandToServer("username: ajsdnas  89y23ghas");
        assertRejectCommand(response);
        response = sendCommandToServer("username: )(^656hh");
        assertRejectCommand(response);
        response = sendCommandToServer("username: look look");
        assertTrue(response.contains("cabin"), "Did not see the name of the current room in response to look");
        response = sendCommandToServer("username: look ashduigiu");
        assertTrue(response.contains("cabin"));
    }

    @Test
    void testInvalidBuildInCommand(){
        String response;
        response = sendCommandToServer("username: Please look around");
        response = sendCommandToServer("username: look cabin");
        assertFalse(response.contains("cabin"));
        sendCommandToServer("username: get axe");
        response = sendCommandToServer("username: inv axe");
        assertFalse(response.contains("axe"));
        response = sendCommandToServer("username: health axe");
        assertFalse(response.contains("3"));
    }

    @Test
    void testBasicAction(){
        String response;
        sendCommandToServer("username: get axe");
        response = sendCommandToServer("username: inv").toLowerCase();
        assertTrue(response.contains("axe"));
        sendCommandToServer("username: goto forest");
        sendCommandToServer("username: get key");
        sendCommandToServer("username: cut tree with axe");
        response = sendCommandToServer("username: look");
        assertTrue(response.contains("log"));
        sendCommandToServer("username: goto cabin");
        sendCommandToServer("username: inv");
        sendCommandToServer("username: open the trapdoor with key");
        response = sendCommandToServer("username: inventory");
        assertFalse(response.contains("key"), "Fail to consume");
        response = sendCommandToServer("username: look");
        assertFalse(response.contains("key"), "Fail to consume");
        response = sendCommandToServer("username: storeroom");
    }

    @Test
    void testExtendedAction(){
        String response;
        sendCommandToServer("username: get axe");
        sendCommandToServer("username: get potion");
        sendCommandToServer("username: get coin");
        response = sendCommandToServer("username: inv").toLowerCase();
        assertTrue(response.contains("axe"));
        sendCommandToServer("username: goto forest");
        sendCommandToServer("username: get key");
        sendCommandToServer("username: cut down tree with axe");
        response = sendCommandToServer("username: look");
        assertTrue(response.contains("log"));
        sendCommandToServer("username: get log");
        sendCommandToServer("username: goto cabin");
        sendCommandToServer("username: inv");
        sendCommandToServer("username: open the trapdoor with key");
        response = sendCommandToServer("username: inventory");
        assertFalse(response.contains("key"), "Fail to consume");
        response = sendCommandToServer("username: look");
        assertFalse(response.contains("key"), "Fail to consume");
        sendCommandToServer("username: goto cellar");
        sendCommandToServer("username: fight with the elf");
        response = sendCommandToServer("username: health");
        assertTrue(response.contains("2"));
        response = sendCommandToServer("username: drink potion");
        assertTrue(response.contains("You drink the potion and your health improves"));
        response = sendCommandToServer("username: health");
        assertTrue(response.contains("3"));
        sendCommandToServer("username: pay the elf with coin");
        response = sendCommandToServer("username: look");
        assertTrue(response.contains("shovel"));
        sendCommandToServer("username: get shovel");
        response = sendCommandToServer("username: inventory");
        assertFalse(response.contains("coin"));
        sendCommandToServer("username: goto cabin");
        sendCommandToServer("username: goto forest");
        sendCommandToServer("username: goto riverbank");
        sendCommandToServer("username: look");
        sendCommandToServer("username: bridge the river with log");
        sendCommandToServer("username: goto clearing");
        response = sendCommandToServer("username: look");
        assertTrue(response.contains("clearing"));
         sendCommandToServer("username: dig the ground with shovel");
        response = sendCommandToServer("username: look");
        assertFalse(response.contains("It looks like the soil has been recently disturbed"));
        assertTrue(response.contains("hole"));
        assertTrue(response.contains("gold"));
        sendCommandToServer("username: get gold");
        response = sendCommandToServer("username: inv");
        assertTrue(response.contains("gold"));
    }

    @Test
    void testInvalidPath(){
        String response;
        sendCommandToServer("username: goto cellar");
        response = sendCommandToServer("username: look");
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
        sendCommandToServer("username: goto forest");
        response = sendCommandToServer("username: look");
        assertTrue(response.contains("cabin"));
        assertTrue(response.contains("riverbank"));
        response = sendCommandToServer("username: goto cabin");
        response = sendCommandToServer("username: look");
    }

    @Test
    void testConsumePath(){
        String response;
        response = sendCommandToServer("username: please destroy path with axe");
        sendCommandToServer("username: goto forest");
        response = sendCommandToServer("username: look");
        assertFalse(response.contains("forest"));
    }

    @Test
    void testPartialCommand(){
        String response;
        // sendCommandToServer("username: get axe");
        sendCommandToServer("username: goto forest");
        sendCommandToServer("username: get key");
        sendCommandToServer("username: goto cabin");
        sendCommandToServer("username: unlock with key");
        sendCommandToServer("username: goto cellar");
        response = sendCommandToServer("username: look");
        assertTrue(response.contains("cellar"));
    }

    @Test
    void testExtraneousEntities(){
        String response;
        sendCommandToServer("username: goto forest");
        sendCommandToServer("username: get key");
        sendCommandToServer("username: goto cabin");
        sendCommandToServer("username: open the trapdoor with key and potion");
        sendCommandToServer("username: goto cellar");
        response = sendCommandToServer("username: look");
        assertFalse(response.contains("cellar"));
    }

    @Test
    void testAmbiguousCommands(){
        String response;
        sendCommandToServer("username: get axe");
        sendCommandToServer("username: goto forest");
        sendCommandToServer("username: get key");
        sendCommandToServer("username: goto cabin");
        sendCommandToServer("username: unlock with key");
        sendCommandToServer("username: look");
        sendCommandToServer("username: destroy with coin");
        sendCommandToServer("username: goto forest");
        response = sendCommandToServer("username: look");
        assertTrue(response.contains("forest"));
        sendCommandToServer("username: goto cabin");
        sendCommandToServer("username: destroy path with coin and axe");
        sendCommandToServer("username: goto forest");
        response = sendCommandToServer("username: look");
        assertFalse(response.contains("forest"));
    }

    @Test
    void testHealthAndRespawn(){
        String response;
        sendCommandToServer("username: get potion");
        sendCommandToServer("username: use potion heal player");
        response = sendCommandToServer("username: health");
        assertTrue(response.contains("3"));
        sendCommandToServer("username: goto forest");
        sendCommandToServer("username: use potion to attack player");
        response = sendCommandToServer("username: health");
        assertTrue(response.contains("2"));
        sendCommandToServer("username: use potion attack player");
        sendCommandToServer("username: use potion attack player");
        response = sendCommandToServer("username: look");
        assertTrue(response.contains("cabin"));
        response = sendCommandToServer("username: inv");
        assertFalse(response.contains("potion"));
        sendCommandToServer("username: goto forest");
        response = sendCommandToServer("username: look");
        assertTrue(response.contains("potion"));
    }

    @Test
    void testConsumeAndProduce(){
        String response;
        sendCommandToServer("username: drink potion");
        response = sendCommandToServer("username: look");
        assertFalse(response.contains("potion"));
        sendCommandToServer("username: storeroom");
        sendCommandToServer("username: use axe to generate");
        response = sendCommandToServer("username: look");
        assertTrue(response.contains("potion"));
    }

    @Test
    void testSeparateKeyphrase(){
        String response;
        sendCommandToServer("username: attack the player");
        response = sendCommandToServer("username: health");
        assertTrue(response.contains("3"));
    }

    @Test
    void testCreateStoreroom(){
        String response;
        sendCommandToServer("username: drink potion");
        response = sendCommandToServer("username: look");
        assertFalse(response.contains("potion"));
        sendCommandToServer("username: use axe to generate");
        response = sendCommandToServer("username: look");
        assertTrue(response.contains("potion"));
    }

    @Test
    void testDuplicateKeyphrase(){
        String response;
        sendCommandToServer("username: goto forest");
        sendCommandToServer("username: get key");
        sendCommandToServer("username: goto cabin");
        response = sendCommandToServer("username: open and unlock the trapdoor with key");
        response = sendCommandToServer("username: inv");
        assertFalse(response.contains("key"));
    }

    @Test
    void testOtherUserInventory(){
        String response;
        sendCommandToServer("usertwo: use potion to attack player");
        sendCommandToServer("userone: get potion");
        sendCommandToServer("usertwo: get potion");
        response = sendCommandToServer("usertwo: inv");
        assertFalse(response.contains("potion"));
        sendCommandToServer("usertwo: drink potion");
        response = sendCommandToServer("usertwo: health");
        assertTrue(response.contains("2"));
    }

    @Test
    void testCompositeCommand(){
        String response;
        sendCommandToServer("username: get axe and potion");
        response = sendCommandToServer("username: inv");
        assertFalse(response.contains("potion"));
        sendCommandToServer("username: goto forest and riverbank");
        response = sendCommandToServer("username: look");
        assertTrue(response.contains("cabin"));
        sendCommandToServer("username: get potion and drink potion");
        response = sendCommandToServer("username: look");
        assertTrue(response.contains("potion"));
    }

    @Test
    void testMultiGet(){
        String response;
        sendCommandToServer("username: get potion");
        sendCommandToServer("username: get potion");
        sendCommandToServer("username: get potion");
        sendCommandToServer("username: drop potion");
        response = sendCommandToServer("username: inv");
        assertFalse(response.contains("potion"));
    }

    @Test
    void testMultiProduce(){
        String response;
        sendCommandToServer("username: use axe to generate");
        sendCommandToServer("username: use axe to generate");
        sendCommandToServer("username: use axe to generate");
        sendCommandToServer("username: get potion");
        response = sendCommandToServer("username: look");
        assertFalse(response.contains("potion"));
    }

    @Test
    void testLocationAsSubject(){
        String response;
        response = sendCommandToServer("username: find cabin");
        assertTrue(response.contains("cabin"));
        sendCommandToServer("username: goto forest");
        response = sendCommandToServer("username: find cabin");
        assertFalse(response.contains("cabin"));
    }
}
