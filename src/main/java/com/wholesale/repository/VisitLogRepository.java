package com.wholesale.repository;

import com.wholesale.model.VisitLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface VisitLogRepository extends JpaRepository<VisitLog, Long> {

    List<VisitLog> findByCustomerIdOrderByVisitedDateDesc(Long customerId);

    List<VisitLog> findByUserIdOrderByVisitedDateDesc(Long userId);

    List<VisitLog> findByVisitedDateOrderByCreatedAtDesc(LocalDate visitedDate);

    boolean existsByCustomerIdAndVisitedDate(Long customerId, LocalDate visitedDate);
}
