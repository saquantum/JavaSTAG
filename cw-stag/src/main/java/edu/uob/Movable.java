package edu.uob;

import edu.uob.Location;

public interface Movable {
    Location getCurrent();
    void setCurrent(Location current);
}
