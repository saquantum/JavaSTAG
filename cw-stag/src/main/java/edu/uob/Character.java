package edu.uob;

public class Character extends GameEntity implements Movable {
    private Location current;

    public Location getCurrent() {
        return current;
    }

    public void setCurrent(Location current) {
        this.current = current;
    }
    public Character(String name) {
        super(name);
    }
}
