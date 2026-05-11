package com.sf.leasing.lead.infrastructure.persistence;

import com.sf.leasing.lead.domain.model.ExceptionQueueRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ExceptionQueueRepository extends JpaRepository<ExceptionQueueRecord, UUID> {

    @Query("SELECT e FROM ExceptionQueueRecord e WHERE e.status = 'OPEN' AND e.cureSlaDeadline < :now")
    List<ExceptionQueueRecord> findAgedOpenEntries(@org.springframework.data.repository.query.Param("now") LocalDateTime now);
}
