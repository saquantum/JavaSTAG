package edu.uob;

public abstract class MovableEntity extends GameEntity{
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
