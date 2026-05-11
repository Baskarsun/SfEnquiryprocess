package com.sf.leasing.lead.infrastructure.persistence;

import com.sf.leasing.lead.domain.model.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, UUID> {

    Optional<Meeting> findByMeetingId(String meetingId);

    List<Meeting> findByProspectId(UUID prospectId);
}
