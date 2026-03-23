package DAJ2EE.demo.repository;

import DAJ2EE.demo.entity.Room;
import DAJ2EE.demo.entity.Service;
import DAJ2EE.demo.entity.ServiceUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceUsageRepository extends JpaRepository<ServiceUsage, Long> {
    List<ServiceUsage> findByRoomId(Long roomId);
    List<ServiceUsage> findByRoomIdAndYear(Long roomId, Integer year);
    List<ServiceUsage> findByMonthAndYear(Integer month, Integer year);

    Optional<ServiceUsage> findByRoomAndServiceAndMonthAndYear(
            Room room, Service service, Integer month, Integer year);

    @Query("SELECT u FROM ServiceUsage u WHERE u.room = :room AND u.service = :service " +
           "AND (u.year < :year OR (u.year = :year AND u.month < :month)) " +
           "ORDER BY u.year DESC, u.month DESC")
    Optional<ServiceUsage> findLatestBefore(@Param("room") Room room, 
                                            @Param("service") Service service, 
                                            @Param("month") Integer month, 
                                            @Param("year") Integer year);

        // Native insert to ensure all required DB columns are provided (some environments have extra NOT NULL columns)
        @org.springframework.transaction.annotation.Transactional
        @org.springframework.data.jpa.repository.Modifying
        @Query(value = "INSERT INTO service_usage (month, new_value, old_value, current_reading, previous_reading, amount, reading_month, reading_year, reading_date, room_id, service_id, year) VALUES (:month, :newValue, :oldValue, :currentReading, :previousReading, :amount, :readingMonth, :readingYear, :readingDate, :roomId, :serviceId, :year)", nativeQuery = true)
        void insertUsage(@Param("month") Integer month,
                                                @Param("newValue") Integer newValue,
                                                @Param("oldValue") Integer oldValue,
                                                @Param("currentReading") Integer currentReading,
                                                @Param("previousReading") Integer previousReading,
                                                @Param("amount") java.math.BigDecimal amount,
                                                @Param("readingMonth") Integer readingMonth,
                                                @Param("readingYear") Integer readingYear,
                                                @Param("readingDate") java.sql.Date readingDate,
                                                @Param("roomId") Long roomId,
                                                @Param("serviceId") Long serviceId,
                                                @Param("year") Integer year);
}
