package edu.uob;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class Location extends GameEntity {

    private Map<String, GameEntity> items = new HashMap<>();

    public Location(String name) {
        super(name);
    }

    public void addItem(GameEntity E){
        if(E == null) return;
        this.items.put(E.getName(), E);
    }

    public boolean hasItem(String name){
        return this.items.containsKey(name);
    }

    public GameEntity removeItem(String name){
        return this.items.remove(name);
    }

   public List<GameEntity> listItems(){
        return this.items.entrySet().stream().map(new Function<Map.Entry<String, GameEntity>, GameEntity>() {
            @Override
            public GameEntity apply(Map.Entry<String, GameEntity> entry) {
                return entry.getValue();
            }
        }).toList();
   }

    public String toString(){
        StringBuilder sb  = new StringBuilder();
        sb.append("Location: ").append(this.getName()).append(", ");
        for (Map.Entry<String, String> entry : this.attributes.entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append(", ");
        }

        for (Map.Entry<String, GameEntity> item : this.items.entrySet()) {
            sb.append(item.getValue().toString()).append(System.lineSeparator());
        }
        return sb.toString();
    }
}
