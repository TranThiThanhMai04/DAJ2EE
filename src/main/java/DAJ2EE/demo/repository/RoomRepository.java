package DAJ2EE.demo.repository;

import DAJ2EE.demo.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findAllByOrderByRoomNumberAsc();

    boolean existsByRoomNumberIgnoreCase(String roomNumber);

    boolean existsByRoomNumberIgnoreCaseAndIdNot(String roomNumber, Long id);
    long countByStatus(DAJ2EE.demo.entity.RoomStatus status);
    List<Room> findByStatus(DAJ2EE.demo.entity.RoomStatus status);
    @Query("SELECT r.status, COUNT(r) FROM Room r GROUP BY r.status")
    List<Object[]> countRoomsByStatus();
}
