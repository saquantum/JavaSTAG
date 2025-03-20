package edu.uob;

import java.util.*;

public class GameAction {

    private Set<String> builtInActions = Set.of("inventory", "inv", "get", "drop", "goto", "look");
}


class Action{

    /**
     * Why is this < String, List < Set < String > > > ?
     *
     * The key String represents the any word from a trigger ( cut, down ),
     * The 2D array of List consists all triggers that starts from the word.
     * Each row is a full combination of trigger ( cut -> cut, cut down; down -> cut down )
     * If `cut` is invalid but `cut down` is a valid trigger,
     * We need access to this 2D array to find out.
     */
    private Map<String, List<Set<String>>> triggers = new HashMap<>();
    private Set<String> subjects = new HashSet<>();
    private Set<String> consumed = new HashSet<>();
    private Set<String> produced = new HashSet<>();

    public Action(){}

    public void setTriggers(Map<String, List<Set<String>>> triggers) {
        this.triggers = triggers;
    }

    public void setSubjects(Set<String> subjects) {
        this.subjects = subjects;
    }

    public void setConsumed(Set<String> consumed) {
        this.consumed = consumed;
    }

    public void setProduced(Set<String> produced) {
        this.produced = produced;
    }

    public boolean mightBeThisAction(String trigger){
        return this.triggers.containsKey(trigger);
    }

    public boolean isThisAction(List<String> triggerPhrase){
        for (String word : triggerPhrase) {
            if(!this.triggers.containsKey(word)) return false;
            List<Set<String>> matrix = this.triggers.get(word);
            for (Set<String> phrase : matrix) {
                for (String s : triggerPhrase) {
                    if(!phrase.contains(s)) break;
                }
                return true;
            }
        }

        return false;
    }
}