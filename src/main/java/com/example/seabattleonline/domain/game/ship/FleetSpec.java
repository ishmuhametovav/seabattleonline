package com.example.seabattleonline.domain.game.ship;

import java.util.Map;

public record FleetSpec(Map<Integer, Integer> countByLength) {
    public static FleetSpec classic() {
        return new FleetSpec(Map.of(
                4, 1,
                3, 2,
                2, 3,
                1, 4)
        );
    }
}
