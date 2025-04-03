package edu.uob;

public class Artefact extends MovableEntity{
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
