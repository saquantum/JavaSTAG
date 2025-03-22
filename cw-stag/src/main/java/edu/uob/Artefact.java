package edu.uob;

public class Artefact extends GameEntity implements Movable {

    private Location current;

    private Player ownedBy;

    public Location getCurrent() {
        return current;
    }

    public void setCurrent(Location current) {
        this.current = current;
    }

    public Player getOwner(){
        return this.ownedBy;
    }

    public void setOwner(Player player){
        this.ownedBy = player;
    }

    public Artefact(String name) {
        super(name);
    }
}
