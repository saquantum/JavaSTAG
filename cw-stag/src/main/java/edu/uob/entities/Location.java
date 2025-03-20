package edu.uob.entities;

import edu.uob.GameEntity;

import java.util.LinkedList;

public class Location extends GameEntity {

    private LinkedList<GameEntity> items = new LinkedList<>();

    public Location(String name) {
        super(name);
    }

    public void addItem(GameEntity E){
        if(E == null) return;
        this.items.add(E);
    }

    public int countObjects(){
        return this.items.size();
    }

    public String toString(){
        StringBuilder sb  = new StringBuilder();
        sb.append("Location: ").append(this.getName()).append(", ");

        this.attributes.forEach((key, value) -> sb.append(key).append(": ").append(value).append(", "));
        sb.append(System.lineSeparator());

        for (GameEntity item : this.items) {
            sb.append(item.toString()).append(System.lineSeparator());
        }
        return sb.toString();
    }
}
