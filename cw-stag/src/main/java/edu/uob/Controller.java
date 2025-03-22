package edu.uob;

import com.alexmerz.graphviz.objects.Edge;

import java.io.File;
import java.util.*;
import java.util.function.Predicate;

public class Controller {
    private final Document document;
    private final GameActions actions;

    public Controller(File entitiesFile, File actionsFile) throws MyExceptions {
        this.document = new Document(entitiesFile);
        this.actions = new GameActions(actionsFile);
    }

    public String handleCommand(String command) {

        // process player name
        Player player = null;
        List<String> seg = Arrays.stream(command.split(":", 2)).toList();
        if (seg.size() == 2) {
            String playerName = seg.get(0);
            for (int i = 0; i < playerName.length(); i++) {
                if (!java.lang.Character.isLetter(playerName.charAt(i)) && playerName.charAt(i) != ' ' && playerName.charAt(i) != '\'' && playerName.charAt(i) != '-') {
                    return "[ERROR]: invalid player name!";
                }
                if (this.document.getPlayers().containsKey(playerName)) {
                    player = this.document.getPlayers().get(playerName);
                } else {
                    player = this.document.newPlayer(playerName);
                }
            }
            command = seg.get(1);
        }
        // no player name specified, try to fetch the default player, if unable then create
        else {
            player = this.document.getPlayers().get("player$default");
            if (player == null) player = this.document.newPlayer("player$default");
        }

        if (player == null) return "[ERROR]: The server's logic is broken and cannot find a valid player?";

        // split commands by white space
        List<String> words = Arrays.stream(command.toLowerCase().split("\\s+")).toList();

        // extract all entities
        Set<String> entities = new HashSet<>();
        for (String word : words) {
            if (this.document.hasEntity(word) || this.document.hasLocation(word)) entities.add(word);
        }

        List<Action> possibleActions = new LinkedList<>();

        // find possible actions by trigger
        for (int i = 0; i < words.size(); i++) {
            if (this.actions.mightBeAction(words.get(i))) {
                possibleActions.addAll(this.actions.possibleActions(words.subList(i, words.size()), entities));
            }
        }

        // loop through possible actions to check subjects
        possibleActions.removeIf(new Predicate<Action>() {
            @Override
            public boolean test(Action action) {
                return !action.checkSubjects(entities);
            }
        });

        if (possibleActions.size() == 0) {
            return "[ERROR]: I can't recognize this command.";
        }

        // Ambiguous commands
        if (possibleActions.size() > 1) {
            return "[ERROR]: The command is too ambiguous and I don't understand what exactly you would like to perform.";
        }

        // only one possible action is left
        Action action = possibleActions.get(0);

        // check subjects must be in player's inventory or in current location
        // if subject is location, there must be a path to that location
        for (String subject : action.getSubjects()) {
            if (subject == null) continue;
            if (this.document.hasLocation(subject)) {
                if (!this.document.hasEdge(player.getCurrent().getName(), subject)) {
                    return "[ERROR]: The target location is inaccessible from current location!";
                }
                continue;
            }
            if (!player.hasItem(subject) && !player.getCurrent().hasItem(subject)) {
                return "[ERROR]: The item is not available to the player!";
            }
        }

        // check consumed and produced must not be in another player's inventory
        for (String consumed : action.getConsumed()) {
            GameEntity consumedObject = this.document.getEntity(consumed);
            if (consumedObject instanceof Artefact && ((Artefact) consumedObject).getOwner() != null && ((Artefact) consumedObject).getOwner() != player) {
                return "[ERROR]: The item to be consumed is owned by another player!";
            }
        }
        for (String produced : action.getProduced()) {
            GameEntity producedObject = this.document.getEntity(produced);
            if (producedObject instanceof Artefact && ((Artefact) producedObject).getOwner() != null && ((Artefact) producedObject).getOwner() != player) {
                return "[ERROR]: The item to be produced is owned by another player!";
            }
        }

        // perform the action
        // basic actions: perform and add narration to action
        if (action instanceof InvAction) {
            StringBuilder sb = new StringBuilder();
            sb.append(player.getName()).append("'s inventory has:").append(System.lineSeparator());
            for (Artefact artefact : player.listInventory()) {
                sb.append(artefact.getName()).append(" ");
            }
            action.setNarration(sb.toString());
        } else if (action instanceof LookAction) {
            StringBuilder sb = new StringBuilder();
            sb.append("Description to current location: ").append(player.getCurrent().getDescription()).append(System.lineSeparator());
            sb.append("You have access to these locations:").append(System.lineSeparator());
            for (Edge edge : this.document.getEdgesFrom(player.getCurrent().getName())) {
                sb.append("  ").append(edge.getTarget().getNode().getId().getId()).append(" ");
            }
            sb.append(System.lineSeparator());
            sb.append("These game entities can be found here:").append(System.lineSeparator());
            for (GameEntity item : player.getCurrent().listItems()) {
                sb.append("  ").append(item.getName()).append(": ").append(item.getDescription()).append(System.lineSeparator());
            }
            sb.append("Other players here:").append(System.lineSeparator());
            for (Map.Entry<String, Player> entry : this.document.getPlayers().entrySet()) {
                if (!entry.getKey().equals(player.getName()) && entry.getValue().getCurrent().equals(player.getCurrent())) {
                    sb.append(entry.getKey()).append(" ");
                }
            }
            action.setNarration(sb.toString());
        } else if (action instanceof GetAction) {
            // move the artefact from current location to storeroom and record it in player's inventory
            String itemName = ((GetAction) action).actsOn();
            // ... but remember to check the item is indeed artefact
            if (!(this.document.getEntity(itemName) instanceof Artefact)) {
                return new StringBuilder().append("[ERROR]: The item ").append(itemName).append(" that you would like to collect is not an artefact!").toString();
            }
            GameEntity item = player.getCurrent().removeItem(itemName);
            this.document.getLocation("storeroom").addItem(item);
            player.insert((Artefact) item);
            action.setNarration(new StringBuilder().append("You picked up the ").append(itemName).toString());
        } else if (action instanceof DropAction) {
            // move the artefact from storeroom to player's location and delete its record
            String itemName = ((DropAction) action).actsOn();
            this.document.getLocation("storeroom").removeItem(itemName);
            Artefact item = player.remove(itemName);
            player.getCurrent().addItem(item);
            action.setNarration(new StringBuilder().append("You dropped the ").append(itemName).toString());
        } else if (action instanceof GotoAction) {
            String locationName = ((GotoAction) action).actsOn();
            Location location = this.document.getLocation(locationName);
            // check target location is indeed a location
            if (location == null)
                return new StringBuilder().append(locationName).append(" is not a valid location!").toString();
            // check there is a path from current location to that location
            if (!this.document.hasEdge(player.getCurrent().getName(), locationName))
                return "No direct path to that location!";
            player.setCurrent(location);
            action.setNarration(new StringBuilder().append("Arrived at new location: ").append(locationName).toString());
        } else if (action instanceof HealthAction) {
            action.setNarration(new StringBuilder().append("Player ").append(player.getName()).append(" is of health ").append(player.getHealth()).toString());
        }

        // custom actions: consume and produce
        else {
            for (String consumed : action.getConsumed()) {
                this.consumeEntity(consumed, player.getCurrent(), player);
            }
            for (String produced : action.getProduced()) {
                this.produceEntity(produced, player.getCurrent(), player);
            }
        }
        // if player died:
        if (player.getHealth() == 0) {
            for (Artefact item : player.listInventory()) {
                player.remove(item.getName());
                player.getCurrent().addItem(item);
            }
            player.setCurrent(this.document.getLocation(0));
            player.resetHealth();
            return "You died and lost all items and teleported to the start location.";
        }

        return action.getNarration();
    }

    public void consumeEntity(String entity, Location currentLocation, Player player) {
        // consume location: remove the path
        if (this.document.hasLocation(entity)) {
            this.document.removeEdge(currentLocation.getName(), entity);
            return;
        }

        // consume health: decrease player's health
        if ("health".equals(entity)) {
            player.decrease();
            return;
        }

        // consume other entity: move it from its location to storeroom
        GameEntity item = ((Movable) this.document.getEntity(entity)).getCurrent().removeItem(entity);
        this.document.getLocation("storeroom").addItem(item);
    }

    public void produceEntity(String entity, Location currentLocation, Player player) {
        // produce location: add a path
        if (this.document.hasLocation(entity)) {
            this.document.addEdge(Util.newEdge(currentLocation.getName(), entity));
            return;
        }

        // produce health: increase player's health
        if ("health".equals(entity)) {
            player.increase();
            return;
        }

        // produce other entity: move it from its location to current location
        GameEntity item = ((Movable) this.document.getEntity(entity)).getCurrent().removeItem(entity);
        currentLocation.addItem(item);
    }
}
