package edu.uob;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public abstract class GameEntity {
    private String name;
    protected Map<String, String> attributes = new HashMap<>();

    public GameEntity(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return this.attributes.get("description");
    }

    public void addAttribute(String attribute, String value) {
        this.attributes.put(attribute, value);
    }

    public void setAttributes(Map<String, String> map) {
        this.attributes = map;
    }

    public boolean equals(GameEntity that) {
        if (that == null) return false;
        return this.getName().equals(that.getName());
    }

    public String toString() {
        String type = null;
        if (this instanceof Artefact) {
            type = "  artefact: ";
        } else if (this instanceof Character) {
            type = "  character: ";
        } else if (this instanceof Furniture) {
            type = "  furniture: ";
        } else if (this instanceof Player) {
            type = "  player: ";
        } else if (this instanceof Location tmp) {
            return tmp.toString();
        }

        StringBuilder sb = new StringBuilder();
        sb.append(type).append(this.getName());

        int size = this.attributes.size();
        int count = 0;
        for (Map.Entry<String, String> entry : this.attributes.entrySet()) {
            sb.append(", ").append(entry.getKey()).append(": ").append(entry.getValue());
            count++;
            if (count == size) {
                sb.append(".");
            }
        }
        return sb.toString();
    }
}

class Location extends GameEntity {

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

abstract class MovableEntity extends GameEntity{
    public MovableEntity(String name) {
        super(name);
    }

    protected Location current;

    public Location getCurrent() {
        return this.current;
    }

    public void setCurrent(Location current) {
        this.current = current;
    }
}

class Artefact extends MovableEntity{
    private Player ownedBy;

    public Player getOwner() {
        return this.ownedBy;
    }

    public void setOwner(Player player) {
        this.ownedBy = player;
    }

    public Artefact(String name) {
        super(name);
    }
}

class Character extends MovableEntity{
    public Character(String name) {
        super(name);
    }
}

class Furniture extends MovableEntity{
    public Furniture(String name) {
        super(name);
    }
}

class Player extends MovableEntity {
    private int health = 3;
    private final Map<String, Artefact> inventory = new HashMap<>();

    public Player(String name) {
        super(name);
    }

    public void insertItem(Artefact artefact) {
        this.inventory.put(artefact.getName(), artefact);
    }

    public boolean hasItem(String name) {
        return this.inventory.containsKey(name);
    }

    public Artefact removeItem(String name) {
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

    public void increase() {
        this.health++;
        if (this.health > 3) this.health = 3;
    }

    public void decrease() {
        this.health--;
        if (this.health < 0) this.health = 0;
    }

    public void resetHealth() {
        this.health = 3;
    }

    public int getHealth() {
        return this.health;
    }
}

