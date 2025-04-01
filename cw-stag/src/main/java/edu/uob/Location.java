package edu.uob;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class Location extends GameEntity {

    private Map<String, MovableEntity> items = new HashMap<>();

    public Location(String name) {
        super(name);
    }

    public void addItem(MovableEntity E) {
        if (E == null) return;
        this.items.put(E.getName(), E);
    }

    public boolean hasItem(String name) {
        return this.items.containsKey(name);
    }

    public MovableEntity removeItem(String name) {
        return this.items.remove(name);
    }

    public List<MovableEntity> listItems() {
        return this.items.entrySet().stream().map(new Function<Map.Entry<String, MovableEntity>, MovableEntity>() {
            @Override
            public MovableEntity apply(Map.Entry<String, MovableEntity> entry) {
                return entry.getValue();
            }
        }).toList();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Location: ").append(this.getName()).append(", ");
        for (Map.Entry<String, String> entry : this.attributes.entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append(", ");
        }

        for (Map.Entry<String, MovableEntity> item : this.items.entrySet()) {
            sb.append(item.getValue().toString()).append(System.lineSeparator());
        }
        return sb.toString();
    }
}
