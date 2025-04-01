package edu.uob;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.*;
import java.util.function.Supplier;

public class GameActions {
    public static final Map<String, Supplier<Action>> builtInActions = Map.ofEntries(
            Map.entry("inventory", InvAction::new), Map.entry("inv", InvAction::new),
            Map.entry("get", GetAction::new), Map.entry("drop", DropAction::new),
            Map.entry("goto", GotoAction::new), Map.entry("look", LookAction::new),
            Map.entry("health", HealthAction::new)
    );

    private final List<Action> customizedActions = new LinkedList<>();

    private final Map<String, List<Action>> lookupActions = new HashMap<>(); // key: first word of trigger phrase, value: corresponding actions

    public GameActions(File actionsFile) throws MyExceptions {
        try {
            NodeList actions = DocumentBuilderFactory.
                    newInstance().
                    newDocumentBuilder().
                    parse(actionsFile).
                    getDocumentElement().
                    getChildNodes();
            this.setGameActions(actions);
        } catch (Exception e) {
            throw new MyExceptions(e.getMessage());
        }
    }

    public boolean mightBeAction(String firstWord) {
        if (GameActions.builtInActions.containsKey(firstWord)) return true;

        if (this.lookupActions.containsKey(firstWord)) return true;

        return false;
    }

    public List<Action> possibleActions(List<String> words, Set<String> entities) {
        String firstWord = words.get(0);
        List<Action> actions = new LinkedList<>();

        // built-in actions
        if (GameActions.builtInActions.containsKey(firstWord)) {
            actions.add(GameActions.builtInActions.get(firstWord).get());
        }
        // custom actions
        else if (this.lookupActions.containsKey(firstWord)) {
            if (!entities.isEmpty()) { // Partial commands: at least one subject
                for (Action action : this.lookupActions.get(firstWord)) {
                    if (action.isThisAction(words)) {
                        actions.add(action);
                    }
                }
            }
        }

        return actions;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Action action : this.customizedActions) {
            sb.append(action.toString()).append(System.lineSeparator());
        }
        return sb.toString();
    }

    private void setGameActions(NodeList actions) {
        for (int i = 1; i < actions.getLength(); i += 2) {
            Element action = (Element) actions.item(i);
            Action actionObject = new Action();

            this.getTriggerElements(action, actionObject);
            this.getEntityElements(action, actionObject, "subjects");
            this.getEntityElements(action, actionObject, "consumed");
            this.getEntityElements(action, actionObject, "produced");
            actionObject.setNarration(action.getElementsByTagName("narration").item(0).getTextContent());

            this.customizedActions.add(actionObject);
            for (String firstWord : actionObject.getFirstWords()) {
                this.insertIntoTable(firstWord, actionObject);
            }
        }
        // check duplicate action triggers
        for (int i = 0; i < this.customizedActions.size(); i++) {
            this.customizedActions.get(i).setIdentifier(i);
        }
    }

    private void getTriggerElements(Element action, Action actionObject) {
        NodeList triggers = ((Element) action.getElementsByTagName("triggers").item(0)).getElementsByTagName("keyphrase");
        for (int j = 0; j < triggers.getLength(); j++) {
            String triggerPhrase = triggers.item(j).getTextContent();
            List<String> phrase = Arrays.stream(triggerPhrase.split("\\s+")).toList();
            actionObject.putTrigger(phrase);
        }
    }

    private void getEntityElements(Element action, Action actionObject, String tagName) {
        NodeList entities = ((Element) action.getElementsByTagName(tagName).item(0)).getElementsByTagName("entity");
        for (int j = 0; j < entities.getLength(); j++) {
            String item = entities.item(j).getTextContent();
            if ("subjects".equals(tagName)) actionObject.addSubject(item);
            else if ("consumed".equals(tagName)) actionObject.addConsumed(item);
            else if ("produced".equals(tagName)) actionObject.addProduced(item);
        }
    }

    private void insertIntoTable(String firstWord, Action actionObject) {
        List<Action> list = new LinkedList<>();
        if (this.lookupActions.containsKey(firstWord)) {
            list = this.lookupActions.get(firstWord);
        }
        list.add(actionObject);
        this.lookupActions.put(firstWord, list);
    }
}