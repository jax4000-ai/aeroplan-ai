package com.aeroplan.scheduling.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.aeroplan.scheduling.domain.ProposalStatus;
import com.aeroplan.scheduling.domain.ScheduleProposal;

public interface ScheduleProposalRepository extends JpaRepository<ScheduleProposal, Long> {
    List<ScheduleProposal> findByStatus(ProposalStatus status);

    Optional<ScheduleProposal> findByStepFunctionsExecutionArn(String executionArn);

    /**
     * Optimistic, conditional transition: only succeeds (returns 1) if the proposal is still
     * PENDING. A second concurrent approve/reject attempt gets 0 rows affected, letting the
     * service layer return 409 Conflict rather than relying on JPA's @Version mechanism.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ScheduleProposal p SET p.status = :newStatus, p.decidedAt = :decidedAt, p.decidedBy = :decidedBy "
            + "WHERE p.id = :id AND p.status = com.aeroplan.scheduling.domain.ProposalStatus.PENDING")
    int decideIfPending(@Param("id") Long id, @Param("newStatus") ProposalStatus newStatus,
            @Param("decidedAt") java.time.Instant decidedAt, @Param("decidedBy") String decidedBy);
}
