package edu.uob;

import java.util.HashMap;
import java.util.Map;

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
