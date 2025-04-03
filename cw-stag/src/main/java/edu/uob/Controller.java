package edu.uob;

import java.io.File;
import java.util.*;
import java.util.function.Predicate;

// [kj24716@it106252 cw-stag]$ ./mvnw clean test -Dtest=edu.uob.MyTests

public class Controller {
    // In document, no Action type variables. In actions, no Entity type variables. Controller is the bridge.
    private final Document document;
    private final GameActions actions;

    public Controller(File entitiesFile, File actionsFile) throws MyExceptions {
        this.document = new Document(entitiesFile);
        this.actions = new GameActions(actionsFile);
    }

    public String handleCommand(String command) throws MyExceptions {
        // process player name
        AbstractMap.SimpleEntry<String, Player> result = this.processPlayer(command);
        command = result.getKey();
        Player player = result.getValue();

        // remove punctuations and split commands by white space
        List<String> words = new LinkedList<>(Arrays.stream(this.removeCommandPunctuations(command).toLowerCase().trim().split("\\s+")).toList());

        // extract all entities
        Set<String> entities = new HashSet<>();
        for (String word : words) {
            if (this.document.hasEntity(word) || this.document.hasLocation(word)) {
                entities.add(word);
            }
        }

        Action action = this.processActions(words, entities).entrySet().stream().toList().get(0).getValue();
        this.checkCustomActions(action, player);
        this.handleCommandActions(action, player, entities);

        // if player died:
        if (player.getHealth() == 0) {
            return this.processPlayerDeath(player);
        }
        return action.getNarration();
    }

    private String processPlayerDeath(Player player) {
        for (Artefact item : player.listInventory()) {
            // set owner of artefact to null, remove artefact from storeroom, remove artefact from inventory, put artefact to location
            item.setOwner(null);
            item.getCurrent().removeItem(item.getName());
            item.setCurrent(player.getCurrent());
            player.removeItem(item.getName());
            player.getCurrent().addItem(item);
        }
        player.setCurrent(this.document.getLocation(0));
        player.resetHealth();
        return "You died and lost all items and are teleported to the start location.";
    }

    private AbstractMap.SimpleEntry<String, Player> processPlayer(String command) throws MyExceptions {
        Player player = null;
        List<String> seg = Arrays.stream(command.split(":", 2)).toList();
        if (seg.size() != 2) throw new MyExceptions.InvalidCommandException("[ERROR]: Player name not specified!");

        String playerName = seg.get(0);
        // check player name does not include invalid chars
        for (int i = 0; i < playerName.length(); i++) {
            if (!java.lang.Character.isLetter(playerName.charAt(i)) && playerName.charAt(i) != ' ' && playerName.charAt(i) != '\'' && playerName.charAt(i) != '-') {
                throw new MyExceptions.InvalidCommandException("[ERROR]: invalid player name!");
            }
        }
        // if player exists already, use it, otherwise create it
        if (this.document.getPlayers().containsKey(playerName)) {
            player = this.document.getPlayers().get(playerName);
        } else {
            player = this.document.newPlayer(playerName);
        }
        command = seg.get(1);
        if (player == null)
            throw new MyExceptions.InvalidCommandException("[ERROR]: The server's logic is broken and cannot find a valid player?");
        return new AbstractMap.SimpleEntry<>(command, player);
    }

    private String removeCommandPunctuations(String command) {
        StringBuilder filteringPunctuation = new StringBuilder();
        for (int i = 0; i < command.length(); i++) {
            if (String.valueOf(command.charAt(i)).matches("[a-zA-Z\\s]")) {
                filteringPunctuation.append(command.charAt(i));
            } else {
                if (i > 0 && java.lang.Character.isLetter(command.charAt(i - 1)) && i < command.length() - 1 && java.lang.Character.isLetter(command.charAt(i + 1))) {
                    filteringPunctuation.append(command.charAt(i));
                }
            }
        }
        return filteringPunctuation.toString();
    }

    private Map<Integer, Action> processActions(List<String> words, Set<String> entities) throws MyExceptions {
        Map<Integer, Action> possibleActions = new HashMap<>();
        // find possible actions by trigger
        for (int i = 0; i < words.size(); i++) {
            if (this.actions.mightBeAction(words.get(i))) {
                List<Action> list = this.actions.possibleActions(words.subList(i, words.size()), entities);
                for (Action action : list) {
                    possibleActions.put(action.getIdentifier(), action);
                }
            }
        }
        // no possible action found
        if (possibleActions.isEmpty()) {
            throw new MyExceptions.InvalidCommandException("[ERROR]: I can't recognize this command.");
        }

        possibleActions = this.checkActionSubjects(words, possibleActions, entities);
        // no valid action found
        if (possibleActions.isEmpty()) {
            throw new MyExceptions.InvalidCommandException("[ERROR]: I can't recognize this command.");
        }
        // Ambiguous commands
        if (possibleActions.size() > 1) {
            throw new MyExceptions.InvalidCommandException("[ERROR]: The command is too ambiguous and I don't understand what exactly you would like to perform.");
        }
        // only one possible action is left
        return possibleActions;
    }

    private Map<Integer, Action> checkActionSubjects(List<String> words, Map<Integer, Action> possibleActions, Set<String> entities) throws MyExceptions {
        try {
            // filter without strict mode
            possibleActions.entrySet().removeIf(new Predicate<Map.Entry<Integer, Action>>() {
                @Override
                public boolean test(Map.Entry<Integer, Action> entry) {
                    return !entry.getValue().checkSubjects(entities, false);
                }
            });
            // if the command is possibly ambiguous, filter again with strict mode
            if(possibleActions.size() > 1){
                possibleActions.entrySet().removeIf(new Predicate<Map.Entry<Integer, Action>>() {
                    @Override
                    public boolean test(Map.Entry<Integer, Action> entry) {
                        return !entry.getValue().checkSubjects(entities, true);
                    }
                });
            }
        } catch (MyExceptions e) {
            throw new MyExceptions.InvalidCommandException(new StringBuilder().append("[ERROR]: ").append(e.getMessage()).toString());
        }
        return possibleActions;
    }

    private void checkCustomActions(Action action, Player player) throws MyExceptions {
        for (String subject : action.getSubjects()) {
            if (subject == null) continue;
            // if subject is location, the player must be at that location
            if (this.document.hasLocation(subject)) {
                if (!subject.equals(player.getCurrent().getName())) {
                    throw new MyExceptions.InvalidCommandException("[ERROR]: The player is not at the subject location!");
                }
                continue;
            }
            // check subjects must be in player's inventory or in current location
            if (!player.hasItem(subject) && !player.getCurrent().hasItem(subject)) {
                throw new MyExceptions.InvalidCommandException("[ERROR]: The item is not available to the player!");
            }
        }

        // check consumed and produced must not be in another player's inventory
        for (String consumed : action.getConsumed()) {
            GameEntity consumedObject = this.document.getEntity(consumed);
            if (consumedObject instanceof Artefact && ((Artefact) consumedObject).getOwner() != null && ((Artefact) consumedObject).getOwner() != player) {
                throw new MyExceptions.InvalidCommandException("[ERROR]: The item to be consumed is owned by another player!");
            }
        }
        for (String produced : action.getProduced()) {
            GameEntity producedObject = this.document.getEntity(produced);
            if (producedObject instanceof Artefact && ((Artefact) producedObject).getOwner() != null && ((Artefact) producedObject).getOwner() != player) {
                throw new MyExceptions.InvalidCommandException("[ERROR]: The item to be produced is owned by another player!");
            }
        }
    }

    private void handleInvAction(Player player, Action action) {
        StringBuilder sb = new StringBuilder();
        sb.append(player.getName()).append("'s inventory has:").append(System.lineSeparator());
        for (Artefact artefact : player.listInventory()) {
            sb.append(artefact.getName()).append(" ");
        }
        action.setNarration(sb.toString());
    }

    private void handleLookAction(Player player, Action action) {
        StringBuilder sb = new StringBuilder();
        sb.append("Description to current location: ").append(player.getCurrent().getDescription()).append(System.lineSeparator());
        if (this.document.getEdgesFrom(player.getCurrent().getName()) != null) {
            sb.append("You have access to these locations:").append(System.lineSeparator());
            for (String to : this.document.getEdgesFrom(player.getCurrent().getName())) {
                sb.append("  ").append(to).append(" ");
            }
        } else {
            sb.append("You have no access to other locations.");
        }
        sb.append(System.lineSeparator());
        sb.append("These game entities can be found here:").append(System.lineSeparator());
        for (GameEntity item : player.getCurrent().listItems()) {
            sb.append(item.toString()).append(System.lineSeparator());
        }
        sb.append("Other players here:").append(System.lineSeparator());
        for (Map.Entry<String, Player> entry : this.document.getPlayers().entrySet()) {
            if (!entry.getKey().equals(player.getName()) && entry.getValue().getCurrent().equals(player.getCurrent())) {
                sb.append(entry.getKey()).append(" ");
            }
        }
        action.setNarration(sb.toString());
    }

    private void handleGetAction(Player player, Action action, String itemName) throws MyExceptions {
        // check the item is indeed artefact
        if (!(this.document.getEntity(itemName) instanceof Artefact)) {
            throw new MyExceptions.InvalidCommandException(new StringBuilder().append("[ERROR]: The item ")
                    .append(itemName).append(" that you would like to collect is not an artefact!").toString());
        }
        // check item is at current location
        if (((Artefact) this.document.getEntity(itemName)).getCurrent() != player.getCurrent()) {
            throw new MyExceptions.InvalidCommandException(new StringBuilder().append("[ERROR]: The item ")
                    .append(itemName).append(" that you would like to collect is not at current location!").toString());
        }
        // transfer the artefact from current location to storeroom
        MovableEntity item = player.getCurrent().removeItem(itemName);
        this.document.getLocation("storeroom").addItem(item);
        // set the location of artefact to be storeroom
        item.setCurrent(this.document.getLocation("storeroom"));
        // record the artefact in player's inventory
        player.insertItem((Artefact) item);
        ((Artefact) item).setOwner(player);
        // set output string
        action.setNarration(new StringBuilder().append("You picked up the ").append(itemName).toString());
    }

    private void handleDropAction(Player player, Action action, String itemName) throws MyExceptions {
        // check the item is indeed artefact
        if (!(this.document.getEntity(itemName) instanceof Artefact)) {
            throw new MyExceptions.InvalidCommandException(new StringBuilder().append("[ERROR]: The item ")
                    .append(itemName).append(" that you would like to drop is not an artefact!").toString());
        }
        // check the artefact can be found in player's inventory and storeroom
        if (!player.hasItem(itemName)) {
            throw new MyExceptions.InvalidCommandException(new StringBuilder().append("[ERROR]: The item ")
                    .append(itemName).append(" that you would like to drop cannot be found in player's inventory!").toString());
        }
        if (!this.document.getLocation("storeroom").hasItem(itemName)) {
            throw new MyExceptions.InvalidCommandException(new StringBuilder().append("[ERROR]: The item ")
                    .append(itemName).append(" that you would like to drop cannot be found!").toString());
        }
        // remove artefact from inventory
        player.removeItem(itemName);
        // remove artefact from storeroom
        Artefact item = (Artefact) this.document.getLocation("storeroom").removeItem(itemName);
        // put artefact to location
        player.getCurrent().addItem(item);
        // reset artefact owner
        item.setOwner(null);
        // set artefact current location
        item.setCurrent(player.getCurrent());
        // set output string
        action.setNarration(new StringBuilder().append("You dropped the ").append(itemName).toString());
    }

    private void handleGotoAction(Player player, Action action, String locationName) throws MyExceptions {
        Location location = this.document.getLocation(locationName);
        // check target location is indeed a location
        if (location == null)
            throw new MyExceptions.InvalidCommandException(new StringBuilder().append(locationName).append(" is not a valid location!").toString());
        // check there is a path from current location to that location
        if (!this.document.hasEdge(player.getCurrent().getName(), locationName)) {
            throw new MyExceptions.InvalidCommandException("[ERROR]: No direct path to that location!");
        }
        player.setCurrent(location);
        action.setNarration(new StringBuilder().append("Arrived at new location: ").append(locationName).toString());
    }

    private void handleCommandActions(Action action, Player player, Set<String> entities) throws MyExceptions {
        // perform basic actions: perform and add narration to action
        if (action instanceof InvAction) {
            this.handleInvAction(player, action);
        } else if (action instanceof LookAction) {
            this.handleLookAction(player, action);
        } else if (action instanceof HealthAction) {
            action.setNarration(new StringBuilder().append("Player ").append(player.getName()).append(" is of health ").append(player.getHealth()).toString());
        } else if (action instanceof GetAction) {
            this.handleGetAction(player, action, entities.iterator().next());
        } else if (action instanceof DropAction) {
            this.handleDropAction(player, action, entities.iterator().next());
        } else if (action instanceof GotoAction) {
            this.handleGotoAction(player, action, entities.iterator().next());
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
        MovableEntity item = ((MovableEntity) this.document.getEntity(entity)).getCurrent().removeItem(entity);
        this.document.getLocation("storeroom").addItem(item);
        // if it is owned by current player, remove it from player's inventory
        if (item instanceof Artefact && ((Artefact) item).getOwner() == player) {
            ((Artefact) item).setOwner(null);
            player.removeItem(item.getName());
        }
        item.setCurrent(this.document.getLocation("storeroom"));
    }

    public void produceEntity(String entity, Location currentLocation, Player player) {
        // produce location: add a path
        if (this.document.hasLocation(entity)) {
            this.document.addEdge(currentLocation.getName(), entity);
            return;
        }

        // produce health: increase player's health
        if ("health".equals(entity)) {
            player.increase();
            return;
        }

        // produce other entity: move it from its location to current location
        MovableEntity item = ((MovableEntity) this.document.getEntity(entity)).getCurrent().removeItem(entity);
        item.setCurrent(currentLocation);
        currentLocation.addItem(item);
        // if it is owned by current player, remove it from player's inventory
        if (item instanceof Artefact && ((Artefact) item).getOwner() == player) {
            ((Artefact) item).setOwner(null);
            player.removeItem(item.getName());
        }
    }
}
