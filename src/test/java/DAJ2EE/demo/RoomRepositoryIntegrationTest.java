package DAJ2EE.demo;

import DAJ2EE.demo.entity.Room;
import DAJ2EE.demo.entity.RoomStatus;
import DAJ2EE.demo.repository.RoomRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class RoomRepositoryIntegrationTest {

    @Autowired
    private RoomRepository roomRepository;

    @Test
    @Transactional
    void saveRoom_shouldPersistWithoutPriceConstraintError() {
        Room room = new Room();
        room.setRoomNumber("T" + UUID.randomUUID().toString().substring(0, 7).toUpperCase());
        room.setMonthlyRent(new BigDecimal("1500000.00"));
        room.setAreaM2(new BigDecimal("14.50"));
        room.setStatus(RoomStatus.EMPTY);
        room.setNote("integration-test");

        Room saved = roomRepository.save(room);

        assertNotNull(saved.getId());
    }
}
