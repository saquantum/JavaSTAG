package edu.uob;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.*;
import java.util.function.BiConsumer;

public class GameActions {
    public static Set<String> builtInActions = Set.of("inventory", "inv", "get", "drop", "goto", "look", "health");

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
        if (GameActions.builtInActions.contains(firstWord)) return true;

        if (this.lookupActions.containsKey(firstWord)) return true;

        return false;
    }

    public List<Action> possibleActions(List<String> words, Set<String> entities) {
        String firstWord = words.get(0);
        List<Action> actions = new LinkedList<>();

        if (GameActions.builtInActions.contains(firstWord)) {
            if ("inv".equals(firstWord) || "inventory".equals(firstWord)) {
                actions.add(new InvAction());
            } else if ("get".equals(firstWord)) {
                actions.add(new GetAction());
            } else if ("drop".equals(firstWord)) {
                actions.add(new DropAction());
            } else if ("goto".equals(firstWord)) {
                actions.add(new GotoAction());
            } else if ("look".equals(firstWord)) {
                actions.add(new LookAction());
            } else if ("health".equals(firstWord)) {
                actions.add(new HealthAction());
            }
        }

        if (this.lookupActions.containsKey(firstWord)) {
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

    public static boolean isBasicAction(Action action) {
        if (action instanceof InvAction || action instanceof GetAction || action instanceof DropAction || action instanceof GotoAction || action instanceof LookAction || action instanceof HealthAction) {
            return true;
        }
        return false;
    }
}


class Action {

    /**
     * Why is this < String, List < List < String > > > ?
     * <p>
     * The key String represents the first word of a trigger ( cut ),
     * The 2D array of String consists all triggers that starts from the word.
     * Each row is a full combination of trigger phrase ( cut -> cut, cut down; chop -> chop; cutdown -> cutdown )
     * If `cut` is invalid but `cut down` is a valid trigger,
     * We need access to this 2D array to find out.
     */
    protected final Map<String, List<List<String>>> triggers = new HashMap<>();
    protected final Set<String> subjects = new HashSet<>();
    protected final List<String> consumed = new LinkedList<>();
    protected final List<String> produced = new LinkedList<>();
    protected String narration;

    protected int identifier; // to filter out duplicate actions

    public Action() {
    }

    // for basic commands
    public Action(List<String> trigger, String subject) {
        this.putTrigger(trigger);
        this.addSubject(subject);
    }

    public int getIdentifier() {
        return identifier;
    }

    public void setIdentifier(int identifier) {
        this.identifier = identifier;
    }

    public void putTrigger(List<String> phrase) {
        List<List<String>> matrix = new LinkedList<>();
        if (this.triggers.containsKey(phrase.get(0))) {
            matrix = this.triggers.get(phrase.get(0));
        }
        matrix.add(phrase);
        this.triggers.put(phrase.get(0), matrix);
    }

    public void addSubject(String subject) {
        this.subjects.add(subject);
    }

    public void addConsumed(String consumed) {
        this.consumed.add(consumed);
    }

    public void addProduced(String produced) {
        this.produced.add(produced);
    }

    public Set<String> getSubjects() {
        return this.subjects;
    }

    public List<String> getConsumed() {
        return this.consumed;
    }

    public List<String> getProduced() {
        return this.produced;
    }

    public void setNarration(String narration) {
        this.narration = narration;
    }

    public String getNarration() {
        return this.narration;
    }

    public List<String> getFirstWords() {
        List<String> list = new LinkedList<>();

        this.triggers.forEach(new BiConsumer<String, List<List<String>>>() {
            @Override
            public void accept(String key, List<List<String>> value) {
                list.add(key);
            }
        });

        return list;
    }

    // cut down tree:
    // 1. `cut` is trigger. then `down` is considered as decorative, no matter whether `cut down` is trigger or not.
    // 2. `cut` is not trigger, must fit both words continuously.
    public boolean isThisAction(List<String> words) {
        // words is the segment that begins from the possible trigger word:
        // `use axe to cut down tree` -> words = `cut down tree`

        List<String> phrase = new LinkedList<>();

        List<List<String>> triggersPossible = this.triggers.get(words.get(0));

        for (String word : words) {
            phrase.add(word);
            for (List<String> validPhrases : triggersPossible) {
                if (Util.ListEquals(validPhrases, phrase)) return true;
            }
        }

        return false;
    }

    public boolean checkSubjects(Set<String> candidates) throws MyExceptions {
        if(this instanceof InvAction || this instanceof LookAction || this instanceof HealthAction){
            if(candidates.size() != 0) throw new MyExceptions.InvalidBasicAction();
            return true;
        }
        if(this instanceof GotoAction || this instanceof GetAction || this instanceof DropAction){
            if(candidates.size() != 1) throw new MyExceptions.InvalidBasicAction();
            return true;
        }
        // Partial command: candidates contains at least one subject
        boolean hasSubject = false;
        for (String subject : this.subjects) {
            if (candidates.contains(subject)) {
                hasSubject = true;
                break;
            }
        }
        if (!hasSubject) return false;

        // Extraneous entity: candidates must be included in subjects
        for (String candidate : candidates) {
            if (!this.subjects.contains(candidate)) return false;
        }

        return true;
    }

    public boolean equals(Action that) {
        return this.getIdentifier() == that.getIdentifier();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("triggers: ");
        this.triggers.forEach(new BiConsumer<String, List<List<String>>>() {
            @Override
            public void accept(String key, List<List<String>> value) {
                for (List<String> strings : value) {
                    for (String string : strings) {
                        sb.append(string).append(" ");
                    }
                    sb.append(", ");
                }
            }
        });
        sb.append(System.lineSeparator());

        sb.append("subjects: ");
        for (String subject : this.subjects) {
            sb.append(subject).append(", ");
        }
        sb.append(System.lineSeparator());

        sb.append("consumed: ");
        for (String consume : this.consumed) {
            sb.append(consume).append(", ");
        }
        sb.append(System.lineSeparator());

        sb.append("produced: ");
        for (String produce : this.produced) {
            sb.append(produce).append(", ");
        }
        sb.append(System.lineSeparator());

        return sb.toString();
    }
}

class InvAction extends Action {
    public InvAction() {
        this.identifier = -1;
    }
}

class GetAction extends Action {
    public GetAction() {
        this.identifier = -2;
    }

    public String actsOn() {
        return this.subjects.stream().toList().get(0);
    }
}

class DropAction extends Action {
    public DropAction() {
        this.identifier = -3;
    }

    public String actsOn() {
        return this.subjects.stream().toList().get(0);
    }
}

class GotoAction extends Action {
    public GotoAction() {
        this.identifier = -4;
    }

    public String actsOn() {
        return this.subjects.stream().toList().get(0);
    }
}

class LookAction extends Action {
    public LookAction() {
        this.identifier = -5;
    }
}

class HealthAction extends Action {
    public HealthAction() {
        this.identifier = -6;
    }
}