package edu.uob;

public class Furniture extends GameEntity implements Movable{
    private Location current;

    public Location getCurrent() {
        return current;
    }

    public void setCurrent(Location current) {
        this.current = current;
    }
    public Furniture(String name) {
        super(name);
    }
}
