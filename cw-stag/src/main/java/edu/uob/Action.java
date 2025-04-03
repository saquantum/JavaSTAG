package edu.uob;

import java.util.*;
import java.util.function.BiConsumer;

public class Action {

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
        if(triggersPossible == null) return false;

        for (String word : words) {
            phrase.add(word);
            for (List<String> validPhrases : triggersPossible) {
                if (Util.ListEquals(validPhrases, phrase)) return true;
            }
        }

        return false;
    }

    public boolean checkSubjects(Set<String> candidates, boolean strict) throws MyExceptions {
        // built-in commands
        if (this instanceof InvAction || this instanceof LookAction || this instanceof HealthAction) {
            if (!candidates.isEmpty()) throw new MyExceptions.InvalidBasicAction();
            return true;
        }
        if (this instanceof GotoAction || this instanceof GetAction || this instanceof DropAction) {
            if (candidates.size() != 1) throw new MyExceptions.InvalidBasicAction();
            return true;
        }
        if(!strict) {
            // Partial command: candidates contains at least one subject
            boolean hasSubject = false;
            for (String subject : this.subjects) {
                if (candidates.contains(subject)) {
                    hasSubject = true;
                    break;
                }
            }
            if (!hasSubject) return false;
        }else{
            // strict mode: all subjects must be found
            for (String subject : this.subjects) {
                if (!candidates.contains(subject)) {
                    return false;
                }
            }
        }
        // Extraneous entity: candidates must be included in subjects
        for (String candidate : candidates) {
            if (!this.subjects.contains(candidate)) return false;
        }

        return true;
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
