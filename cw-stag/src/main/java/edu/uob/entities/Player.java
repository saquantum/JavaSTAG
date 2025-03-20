package edu.uob.entities;

import edu.uob.GameEntity;

import java.util.HashMap;
import java.util.Map;

public class Player extends GameEntity {
    private Map<String, String> attributes = new HashMap<>();

    public Player(String name) {
        super(name);
    }
}
