package com.example.seabattleonline.repository.impl;

import com.example.seabattleonline.domain.exception.RoomAlreadyExistsException;
import com.example.seabattleonline.domain.room.Room;
import com.example.seabattleonline.repository.RoomRepository;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

//TODO player should not be in several rooms at one time
//TODO player cant access room what he does not belong to
@Repository
public class InMemoryRoomRepositoryImpl implements RoomRepository {

    private final Map<Long, Room> rooms = new HashMap<>();
    private final AtomicLong nextId = new AtomicLong(0);

    @Override
    public Room save(final Room room) {
        if (room.getId() != null && rooms.containsKey(room.getId())) {
            throw new RoomAlreadyExistsException(
                    "Room " + room.getId() + " already exists"
            );
        }
        room.setId(nextId.getAndIncrement());
        rooms.put(room.getId(), room);
        return room;
    }

    @Override
    public Optional<Room> findById(final Long id) {
        if (!rooms.containsKey(id)) {
            return Optional.empty();
        }
        return Optional.of(rooms.get(id));
    }

    @Override
    public void deleteById(final Long id) {
        rooms.remove(id);
    }
}
