package DAJ2EE.demo.service;

import DAJ2EE.demo.entity.Room;
import DAJ2EE.demo.exception.ResourceNotFoundException;
import DAJ2EE.demo.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoomServiceImpl implements RoomService {

    @Autowired
    private RoomRepository roomRepository;

    @Override
    public List<Room> getAllRooms() {
        return roomRepository.findAllByOrderByRoomNumberAsc();
    }

    @Override
    public Room getRoomById(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phòng với ID: " + id));
    }

    @Override
    @Transactional
    public Room createRoom(Room room) {
        String roomNumber = normalizeRoomNumber(room.getRoomNumber());
        if (roomRepository.existsByRoomNumberIgnoreCase(roomNumber)) {
            throw new IllegalArgumentException("Mã phòng đã tồn tại trong hệ thống");
        }

        room.setRoomNumber(roomNumber);
        normalizeBusinessFields(room);
        return roomRepository.save(room);
    }

    @Override
    @Transactional
    public Room updateRoom(Long id, Room updatedRoom) {
        Room existing = getRoomById(id);

        String roomNumber = normalizeRoomNumber(updatedRoom.getRoomNumber());
        if (roomRepository.existsByRoomNumberIgnoreCaseAndIdNot(roomNumber, id)) {
            throw new IllegalArgumentException("Mã phòng đã tồn tại trong hệ thống");
        }

        existing.setRoomNumber(roomNumber);
        existing.setMonthlyRent(updatedRoom.getMonthlyRent());
        existing.setAreaM2(updatedRoom.getAreaM2());
        existing.setStatus(updatedRoom.getStatus());
        existing.setNote(updatedRoom.getNote());

        normalizeBusinessFields(existing);
        return roomRepository.save(existing);
    }

    @Override
    @Transactional
    public void deleteRoom(Long id) {
        Room room = getRoomById(id);
        roomRepository.delete(room);
    }

    private String normalizeRoomNumber(String roomNumber) {
        if (roomNumber == null) {
            return null;
        }
        return roomNumber.trim().toUpperCase();
    }

    // Normalize free-text fields before persistence.
    private void normalizeBusinessFields(Room room) {
        if (room.getNote() != null) {
            room.setNote(room.getNote().trim());
        }
    }
}
