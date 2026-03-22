package DAJ2EE.demo.service;

import DAJ2EE.demo.entity.Room;
import java.util.List;
import java.util.Optional;

public interface RoomService {
    List<Room> getAllRooms();
    Optional<Room> getRoomById(Long id);
    Room saveRoom(Room room);
    void deleteRoom(Long id);
    boolean existsByRoomNumber(String roomNumber);
}
