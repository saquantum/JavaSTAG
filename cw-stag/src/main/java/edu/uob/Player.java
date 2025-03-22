package edu.uob;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class Player extends GameEntity implements Movable{
    private int health = 3;
    private final Map<String, Artefact> inventory = new HashMap<>();
    private Location current;

    public Player(String name) {
        super(name);
    }

    public void insert(Artefact artefact) {
        this.inventory.put(artefact.getName(), artefact);
    }

    public boolean hasItem(String name) {
        return this.inventory.containsKey(name);
    }

    public Artefact remove(String name) {
        return this.inventory.remove(name);
    }

    public List<Artefact> listInventory() {
        return this.inventory.entrySet().stream().map(new Function<Map.Entry<String, Artefact>, Artefact>() {
            @Override
            public Artefact apply(Map.Entry<String, Artefact> entry) {
                return entry.getValue();
            }
        }).toList();
    }

    public void increase(){
        this.health++;
        if(this.health > 3) this.health = 3;
    }

    public void decrease(){
        this.health--;
        if(this.health < 0) this.health = 0;
    }

    public void resetHealth(){
        this.health = 3;
    }

    public int getHealth(){
        return this.health;
    }

    @Override
    public Location getCurrent() {
        return this.current;
    }

    @Override
    public void setCurrent(Location current) {
        this.current = current;
    }
}
