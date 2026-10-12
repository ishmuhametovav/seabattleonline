package com.example.seabattleonline.domain.game.ship;

import com.example.seabattleonline.domain.game.exception.ShipAlreadyDestroyedException;
import com.example.seabattleonline.domain.game.vector.Vector;

import java.util.List;

public class Ship {
    private final List<Vector> ship;
    private int undamaged;
    private final boolean horizontal;

    public Ship(final List<Vector> ship, final boolean horizontal) {
        this.ship = ship;
        this.undamaged = ship.size();
        this.horizontal = horizontal;
    }

    public void hit() {
        if (undamaged == 0) {
            throw new ShipAlreadyDestroyedException();
        }
        undamaged--;
    }

    public List<Vector> getCells() {
        return List.copyOf(ship);
    }

    public boolean isDestroyed() {
        return undamaged == 0;
    }
}
