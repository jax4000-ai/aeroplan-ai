package com.aeroplan.scheduling.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aeroplan.scheduling.domain.ScheduleAssignment;

public interface ScheduleAssignmentRepository extends JpaRepository<ScheduleAssignment, Long> {
    List<ScheduleAssignment> findByScheduleVersionId(Long scheduleVersionId);
}
