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

    void assertRejectCommand(String response) {
        response = response.toLowerCase();
        assertTrue(response.contains("error") || response.contains("reject") || response.contains("cannot")
                || response.contains("can't") || response.contains("cant") || response.contains("don't")
                || response.contains("dont") || response.contains("unknown") || response.contains("recogni")
                || response.contains("invalid") || response.contains("refuse") || response.contains("unauthori")
                || response.contains("unreachable") || response.contains("do not"));
    }

    void assertPassCommand(String response) {
        response = response.toLowerCase();
        assertFalse(response.contains("error") || response.contains("reject") || response.contains("cannot")
                || response.contains("can't") || response.contains("cant") || response.contains("don't")
                || response.contains("dont") || response.contains("unknown") || response.contains("recogni")
                || response.contains("invalid") || response.contains("refuse") || response.contains("unauthori")
                || response.contains("unreachable") || response.contains("do not"));
    }

    // parsing entities:
    // check the first location.
    // create a storeroom if not included in the dot file.
    @Test
    void testParsingEntity() {
        assertTrue(sendCommandToServer("me: look").contains("cabin"));
        sendCommandToServer("me: store key");
        assertFalse(sendCommandToServer("me: look").contains("key"));
        sendCommandToServer("me: retrieve with axe");
        assertTrue(sendCommandToServer("me: look").contains("key"));
    }

    // inv: print all items from current player. Check: All items must be in current player's inventory and no extra item.
    @Test
    void testInv() {
        sendCommandToServer("you: get  Axe;");
        sendCommandToServer("me: geT potion;");
        String response = sendCommandToServer("you: check my inv!");
        assertTrue(response.contains("axe") && !response.contains("potion"));

        response = sendCommandToServer("me: inventory check");
        assertTrue(!response.contains("axe") && response.contains("potion"));
    }

    // get: picks up an artefact from current location. Check: Cannot get other types of entity. Cannot get artefact not at this location. Cannot get artefact owned by another player. Cannot get artefact non-existed.
    @Test
    void testGet() {
        assertRejectCommand(sendCommandToServer("me: get trapdoor "));
        assertRejectCommand(sendCommandToServer("me: get   cabin"));
        assertRejectCommand(sendCommandToServer("me: get horn"));
        assertRejectCommand(sendCommandToServer("me: get key"));
        assertRejectCommand(sendCommandToServer("me: get your ass"));

        sendCommandToServer("me: goto Forest");
        sendCommandToServer("me: key get.");
        assertTrue(sendCommandToServer("me: inv").contains("key"));
        sendCommandToServer("me: Goto caBin");
        sendCommandToServer("you: goto forest");
        assertTrue(sendCommandToServer("you: look").contains("forest"));
        assertRejectCommand(sendCommandToServer("you: get key"));
        sendCommandToServer("you: goto cabin");
        assertRejectCommand(sendCommandToServer("you: open trapdoor"));
        assertFalse(sendCommandToServer("me: look").contains("cellar"));
        sendCommandToServer("me: unlock and open trapdoor or unlock then open key");
        assertTrue(sendCommandToServer("you: look").contains("cellar"));
        sendCommandToServer("me: retrieve axe");
        assertTrue(sendCommandToServer("you: look").contains("key"));
    }

    @Test
    void testDuplicateGet() {
        sendCommandToServer("me: get and get then get axe");
        assertTrue(sendCommandToServer("me: inv").contains("axe"));
        assertRejectCommand(sendCommandToServer("me: get axe"));
        assertRejectCommand(sendCommandToServer("me: get and get axe"));
        assertRejectCommand(sendCommandToServer("me: get axe"));
    }

    // drop: drop an artefact from current player. Check: The item is indeed an artefact. The artefact is in current player's inventory. The artefact is transferred to current location.
    @Test
    void testDrop() {
        sendCommandToServer("me: get trapdoor");
        assertRejectCommand(sendCommandToServer("me: drop trapdoor"));
        assertRejectCommand(sendCommandToServer("me: drop axe"));
        assertRejectCommand(sendCommandToServer("me: drop horn"));

        sendCommandToServer("me: get axe");
        sendCommandToServer("me: goto forest");
        sendCommandToServer("me: drop and drop then drop axe");
        assertRejectCommand(sendCommandToServer("me: drop axe"));
        assertRejectCommand(sendCommandToServer("me: drop axe"));
        assertTrue(sendCommandToServer("me: look").contains("axe"));
        assertFalse(sendCommandToServer("me: inv").contains("axe"));
        assertFalse(sendCommandToServer("you: look").contains("axe"));
        sendCommandToServer("you: goto forest");
        String response = sendCommandToServer("you: look");
        assertTrue(response.contains("axe") && response.contains("forest"));
        sendCommandToServer("you: get axe");
        assertTrue(sendCommandToServer("you: inv").contains("axe"));
        assertFalse(sendCommandToServer("you: look").contains("axe"));
        sendCommandToServer("you: drop axe");
        assertTrue(sendCommandToServer("me: look").contains("axe"));
        assertFalse(sendCommandToServer("me: look").contains("log"));
        sendCommandToServer("me: cut tree");
        assertTrue(sendCommandToServer("me: look").contains("log"));
    }

    // goto: goto a new location. Check: A direct path from current location to target location exists. Only current player is transferred to new location.
    // check if location is consumed, only one way route is removed.
    // check if location is produced, only one way route is added.
    @Test
    void testGoto(){
        assertRejectCommand(sendCommandToServer("me: goto riverbank"));
        sendCommandToServer("you: goto forest");
        assertTrue(sendCommandToServer("you: look").contains("cabin"));
        assertTrue(sendCommandToServer("me: look").contains("forest"));
        sendCommandToServer("me: close path with axe");
        assertFalse(sendCommandToServer("me: look").contains("forest"));
        assertRejectCommand(sendCommandToServer("me: goto forest"));
        assertTrue(sendCommandToServer("you: look").contains("cabin"));
        sendCommandToServer("you: goto cabin");
        assertFalse(sendCommandToServer("you: look").contains("forest"));

        sendCommandToServer("who: get axe");
        sendCommandToServer("who: open axe");
        sendCommandToServer("who: goto forest");
        sendCommandToServer("who: goto riverbank");
        sendCommandToServer("who: goto lab");
        String response = sendCommandToServer("who: look");
        assertTrue(!response.contains("forest") && response.contains("lab"));

        sendCommandToServer("who: goto labmachine");
        response = sendCommandToServer("who: look");
        assertTrue(response.contains("11") && response.contains("version"));
        sendCommandToServer("who: goto lab");

        sendCommandToServer("who: open axe");
        response = sendCommandToServer("who: look");
        assertTrue(response.contains("forest") && response.contains("lab"));
        sendCommandToServer("who: goto forest");
        response = sendCommandToServer("who: look");
        assertTrue(response.contains("forest") && !response.contains("lab"));
    }

    // look: print all items at current location. Check: The location is the location where the specified player is at. Other players at this location are printed. All items should be included.
    @Test
    void testLook(){
        sendCommandToServer("pla yer: get axe");
        String response = sendCommandToServer("pla-yer: look");
        assertTrue(response.contains("cabin") && response.contains("forest") && !response.contains("riverbank") && response.contains("magic potion") && response.contains("locked wooden") && !response.contains("axe") && response.contains("pla yer") && !response.contains("pla-yer"));
        sendCommandToServer("pla yer: goto forest");
        sendCommandToServer("pla yer: drop axe");
        response = sendCommandToServer("pla yer: look");
        assertTrue(response.contains("forest") && response.contains("cabin") && response.contains("riverbank") && response.contains("pine tree") && response.contains("old key") && response.contains("razor sharp") && !response.contains("pla yer") && !response.contains("pla-yer") );
    }

    // health: print current player's health. Check: Not health of another player is printed.
    @Test
    void testHealth(){
        sendCommandToServer("me: get potion");
        sendCommandToServer("me: drink poison potion");
        assertTrue(sendCommandToServer("me: health").contains("2"));
        assertTrue(sendCommandToServer("you: health").contains("3"));
        sendCommandToServer("me: drink poison potion");
        assertTrue(sendCommandToServer("me: health").contains("1"));
        assertTrue(sendCommandToServer("you: health").contains("3"));
        sendCommandToServer("me: drink poison potion");
        assertTrue(sendCommandToServer("me: health").contains("3"));
        assertTrue(sendCommandToServer("you: health").contains("3"));
        sendCommandToServer("you: drink poison potion");
        assertTrue(sendCommandToServer("me: health").contains("3"));
        assertTrue(sendCommandToServer("you: health").contains("2"));
    }

    // custom actions
    // check subjects are in the inventory of current player or at current location.
    // check consumed and produced items are not in another player's inventory.
    @Test
    void testActionSubjects(){
        assertPassCommand(sendCommandToServer("me: test cabin"));
        sendCommandToServer("me: goto forest");
        assertRejectCommand(sendCommandToServer("me: test cabin"));
        sendCommandToServer("me: goto cabin");

        sendCommandToServer("me: get axe");
        sendCommandToServer("me: goto forest");
        sendCommandToServer("you: goto forest");
        assertRejectCommand(sendCommandToServer("you: cut tree"));
        sendCommandToServer("me: drop axe");
        sendCommandToServer("you: cut tree");
        assertTrue(sendCommandToServer("me: look").contains("log"));

        sendCommandToServer("me: get key");
        sendCommandToServer("you: get axe");
        sendCommandToServer("me: goto cabin");
        sendCommandToServer("you: goto cabin");
        assertRejectCommand(sendCommandToServer("you: store key"));
        assertRejectCommand(sendCommandToServer("me: retrieve axe"));
        sendCommandToServer("me: drop key");
        assertRejectCommand(sendCommandToServer("me: retrieve axe"));
        assertTrue(sendCommandToServer("you: look").contains("key"));
        sendCommandToServer("you: retrieve axe");
        assertTrue(sendCommandToServer("you: look").contains("key"));
        sendCommandToServer("you: get key");
        assertFalse(sendCommandToServer("you: look").contains("key"));
        sendCommandToServer("you: retrieve axe");
        assertTrue(sendCommandToServer("you: look").contains("key"));
        sendCommandToServer("you: retrieve axe");
        sendCommandToServer("you: retrieve axe");
        assertTrue(sendCommandToServer("you: look").contains("key"));
        sendCommandToServer("you: store key");
        assertFalse(sendCommandToServer("you: look").contains("key"));
        assertRejectCommand(sendCommandToServer("you: store key"));
        assertRejectCommand(sendCommandToServer("you: store key"));
    }

    // check: case insensitivity.
    @Test
    void testCaseInsensitivity() {
        sendCommandToServer("Me: GeT The AxE.");
        assertTrue(sendCommandToServer("Me: InV").contains("axe"));
        assertFalse(sendCommandToServer("me: inv").contains("axe"));
        sendCommandToServer("Me: GoTo ForEst.");
        assertRejectCommand(sendCommandToServer("me: cut tree"));
        sendCommandToServer("Me: Tree cuT AXE.");
        assertTrue(sendCommandToServer("Me: LoOk").contains("log"));
    }

    // check decorative words.
    @Test
    void testDecorative() {
        sendCommandToServer("me: you get my axe now");
        assertFalse(sendCommandToServer("me: look").contains("axe"));
        assertTrue(sendCommandToServer("me: inv").contains("axe"));
        sendCommandToServer("me: don't drop me axe nmsl");
        assertFalse(sendCommandToServer("me: inv").contains("axe"));
        assertTrue(sendCommandToServer("me: look").contains("axe"));
    }

    // check partial commands.
    @Test
    void testPartial() {
        sendCommandToServer("me: get axe");
        sendCommandToServer("me: goto forest");
        assertRejectCommand(sendCommandToServer("me: cut down"));
        assertTrue(sendCommandToServer("me: look").contains("tree"));
        sendCommandToServer("me: cut tree");
        assertTrue(sendCommandToServer("me: look").contains("log"));
    }

    // check word ordering.
    @Test
    void testOrdering() {
        sendCommandToServer("me: forest goto");
        sendCommandToServer("me: key to get");
        sendCommandToServer("me: cabin the goto");
        sendCommandToServer("me: trapdoor to unlock with the key");
        assertTrue(sendCommandToServer("me: look").contains("cellar"));
    }

    // check extra entity.
    @Test
    void testExtraneous() {
        assertRejectCommand(sendCommandToServer("me: open path to forest with axe"));
        assertTrue(sendCommandToServer("me: open path with axe").contains("way"));
    }

    // check ambiguous commands: write more than one 'open' action
    @Test
    void testAmbiguous() {
        assertRejectCommand(sendCommandToServer(""));
        assertRejectCommand(sendCommandToServer("  "));

        assertTrue(sendCommandToServer("me: open axe").contains("way"));
        assertTrue(sendCommandToServer("me: open axe").contains("way"));
        assertTrue(sendCommandToServer("me: open axe").contains("way"));
        sendCommandToServer("me: goto forest");
        sendCommandToServer("me: get key");
        sendCommandToServer("me: goto cabin");
        assertTrue(sendCommandToServer("me: open axe").contains("way"));
        assertTrue(sendCommandToServer("me: open key").contains("door"));
    }

    @Test
    void testDuplicateCommand(){
        assertRejectCommand(sendCommandToServer("me: goto cabin"));
        assertTrue(sendCommandToServer("me: open axe").contains("way"));
        assertTrue(sendCommandToServer("me: open axe").contains("way"));
        assertTrue(sendCommandToServer("me: open axe").contains("way"));
        assertTrue(sendCommandToServer("me: close axe").contains("way"));
        assertRejectCommand(sendCommandToServer("me: goto forest"));
    }

    // check composite commands: 'get potion and axe', 'get potion and unlock trapdoor'
    @Test
    void testComposite() {
        assertRejectCommand(sendCommandToServer("me: look and get potion"));
        assertRejectCommand(sendCommandToServer("me: get potion and axe"));
        assertRejectCommand(sendCommandToServer("me: get potion and open axe"));
    }

    // check player name
    @Test
    void testInvalidPlayerName() {
        assertRejectCommand(sendCommandToServer("get potion"));
        assertRejectCommand(sendCommandToServer("goto forest"));
        assertRejectCommand(sendCommandToServer("me_: look"));
        assertRejectCommand(sendCommandToServer("me$: look"));
        assertRejectCommand(sendCommandToServer("player1: look"));
        assertRejectCommand(sendCommandToServer("pla*yer: look"));
    }

    // if player died, all items should be dropped and player transited to initial location.
    @Test
    void testPlayerHealth() {
        sendCommandToServer("me: get potion");
        sendCommandToServer("me: drink healing potion");
        assertTrue(sendCommandToServer("me: health").contains("3"));
        sendCommandToServer("me: potion drink poison");
        assertTrue(sendCommandToServer("me: health").contains("2"));
        sendCommandToServer("me: get axe");
        sendCommandToServer("me: potion drink healing");
        assertTrue(sendCommandToServer("me: health").contains("3"));
        sendCommandToServer("me: drink double healing potion");
        assertTrue(sendCommandToServer("me: health").contains("3"));
        sendCommandToServer("me: potion drink double poison");
        assertTrue(sendCommandToServer("me: health").contains("1"));
        sendCommandToServer("me: goto forest");
        sendCommandToServer("me: potion drink poison");
        String response = sendCommandToServer("me: look");
        assertTrue(response.contains("cabin") && !response.contains("potion") && !response.contains("axe"));
        assertTrue(sendCommandToServer("me: health").contains("3"));
        sendCommandToServer("me: goto forest");
        response = sendCommandToServer("me: look");
        assertTrue(response.contains("forest") && response.contains("potion") && response.contains("axe"));

        sendCommandToServer("you: goto forest");
        assertTrue(sendCommandToServer("you: cut tree").contains("cut down"));
        sendCommandToServer("you: get potion");
        assertTrue(sendCommandToServer("you: inv").contains("potion"));
    }



}
