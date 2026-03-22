package DAJ2EE.demo.service;

import DAJ2EE.demo.entity.Room;

import java.util.List;

public interface RoomService {
    List<Room> getAllRooms();

    Room getRoomById(Integer id);

    Room createRoom(Room room);

    Room updateRoom(Integer id, Room room);

    void deleteRoom(Integer id);
}
