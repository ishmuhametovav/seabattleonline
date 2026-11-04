package com.example.seabattleonline.repository;

import com.example.seabattleonline.domain.room.Room;

import java.util.Optional;

public interface RoomRepository {

    Room save(Room room);

    Optional<Room> findById(Long id);

    void deleteById(Long id);
}
