package com.example.seabattleonline.domain.room;

import lombok.Data;

@Data
public class Room {
    private Long id;
    private Long firstUserId;
    private Long secondUserId;
    private Status status;
    private CurrentTurn currentTurn;
}
