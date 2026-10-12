package com.example.seabattleonline.domain.game.board;

import com.example.seabattleonline.domain.game.exception.BadShipPlacementException;
import com.example.seabattleonline.domain.game.exception.ShipLengthNotAvailableException;
import com.example.seabattleonline.domain.game.exception.ShotOutOfBoundsException;
import com.example.seabattleonline.domain.game.exception.TooManyShipsException;
import com.example.seabattleonline.domain.game.ship.FleetSpec;
import com.example.seabattleonline.domain.game.ship.Ship;
import com.example.seabattleonline.domain.game.ship.ShotResult;
import com.example.seabattleonline.domain.game.vector.Vector;

import java.util.*;

public class Board {
    private final CellState[][] grid;
    private final int[][] shipIds;
    private final List<Ship> ships = new ArrayList<>();
    private final FleetSpec fleetSpec;
    private Map<Integer, Integer> remaining;

    public Board(final int size, final FleetSpec fleetSpec) {
        if (size <= 0) {
            throw new IllegalArgumentException("Board size must be positive");
        }
        this.fleetSpec = fleetSpec;
        remaining = new HashMap<>(fleetSpec.countByLength());
        grid = new CellState[size][size];
        shipIds = new int[size][size];
        for (CellState[] row : grid) {
            Arrays.fill(row, CellState.EMPTY);
        }
        for (int[] row : shipIds) {
            Arrays.fill(row, -1);
        }
    }

    public void placeShip(
            final Vector start,
            final int length,
            final boolean horizontal
    ) {
        if (!remaining.containsKey(length)) {
            throw new ShipLengthNotAvailableException();
        }
        if (remaining.get(length) == 0) {
            throw new TooManyShipsException();
        }
        List<Vector> coords = buildCoords(start, length, horizontal);
        if (!canPlaceShip(coords)) {
            throw new BadShipPlacementException();
        }
        remaining.replace(length, remaining.get(length) - 1);
        int shipId = ships.size();
        ships.add(new Ship(coords, horizontal));
        for (Vector v : coords) {
            grid[v.y()][v.x()] = CellState.SHIP;
            shipIds[v.y()][v.x()] = shipId;
        }
    }

    public ShotResult shot(final Vector coords) {
        if (!inBounds(coords.x(), coords.y())) {
            throw new ShotOutOfBoundsException();
        }

        ShotResult result = ShotResult.MISS;
        CellState value = grid[coords.y()][coords.x()];

        if (value == CellState.SHIP) {
            Ship ship = ships.get(shipIds[coords.y()][coords.x()]);
            ship.hit();
            grid[coords.y()][coords.x()] = CellState.HIT;
            if (ship.isDestroyed()) {
                markShipAsDestroyed(ship);
            }
            result = ShotResult.SHOT;
        } else if (value == CellState.MISS
                || value == CellState.HIT
                || value == CellState.SUNK
        ) {
            result = ShotResult.ALREADY_SHOT;
        } else {
            grid[coords.y()][coords.x()] = CellState.MISS;
        }
        return result;
    }

    public boolean allShipsPlaced() {
        int count = remaining.values().stream().mapToInt(i -> i).sum();
        return count == 0;
    }

    public boolean allShipsDestroyed() {
        for (Ship ship : ships) {
            if (!ship.isDestroyed()) {
                return false;
            }
        }
        return true;
    }

    public void clear() {
        ships.clear();
        for (CellState[] row : grid) {
            Arrays.fill(row, CellState.EMPTY);
        }
        for (int[] row : shipIds) {
            Arrays.fill(row, -1);
        }
        remaining = new HashMap<>(fleetSpec.countByLength());
    }

    public List<CellView> getGrid() {
        List<CellView> result = new ArrayList<>();
        for (int y = 0; y < grid.length; y++) {
            for (int x = 0; x < grid[0].length; x++) {
                CellState state = grid[y][x];
                int shipId = shipIds[y][x];
                result.add(new CellView(x, y, state, shipId));
            }
        }
        return result;
    }

    public List<CellView> getFilteredGrid() {
        List<CellView> result = new ArrayList<>();
        for (int y = 0; y < grid.length; y++) {
            for (int x = 0; x < grid[0].length; x++) {
                CellState state = grid[y][x];
                CellState visible = (state == CellState.SHIP)
                        ? CellState.EMPTY : state;
                result.add(new CellView(x, y, visible, -1));
            }
        }
        return result;
    }

    private void markShipAsDestroyed(final Ship ship) {
        List<Vector> cells = ship.getCells();
        for (var cell : cells) {
            grid[cell.y()][cell.x()] = CellState.SUNK;
        }
    }

    private boolean canPlaceShip(final List<Vector> coords) {
        for (Vector v : coords) {
            if (!correctCellPosition(v)) {
                return false;
            }
        }
        return true;
    }

    private List<Vector> buildCoords(
            final Vector start,
            final int length,
            final boolean horizontal
    ) {
        List<Vector> coords = new ArrayList<>(length);
        Vector dir = horizontal ? new Vector(1, 0) : new Vector(0, 1);
        for (int i = 0; i < length; i++) {
            coords.add(new Vector(
                    start.x() + dir.x() * i,
                    start.y() + dir.y() * i)
            );
        }
        return coords;
    }

    private boolean correctCellPosition(final Vector coords) {
        int x = coords.x();
        int y = coords.y();

        if (!inBounds(x, y) || grid[y][x] == CellState.SHIP) {
            return false;
        }

        Vector[] dir = {
                new Vector(-1, -1), new Vector(-1, 0),
                new Vector(-1, 1), new Vector(0, 1),
                new Vector(1, 1), new Vector(1, 0),
                new Vector(1, -1), new Vector(0, -1)
        };

        for (Vector v : dir) {
            int newX = x + v.x();
            int newY = y + v.y();
            if (inBounds(newX, newY) && grid[newY][newX] == CellState.SHIP) {
                return false;
            }
        }

        return true;
    }

    private boolean inBounds(final int x, final int y) {
        return x >= 0 && x < grid[0].length && y >= 0 && y < grid.length;
    }
}
