package com.sf.leasing.lead.infrastructure.persistence;

import com.sf.leasing.lead.domain.model.Interaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InteractionRepository extends JpaRepository<Interaction, UUID> {

    List<Interaction> findByLeadIdOrderByInteractionTimestampAsc(UUID leadId);
}
