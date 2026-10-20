package com.aeroplan.scheduling.api;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aeroplan.scheduling.domain.MachineDowntime;
import com.aeroplan.scheduling.domain.ProposalStatus;
import com.aeroplan.scheduling.domain.ScheduleProposal;
import com.aeroplan.scheduling.repository.MachineDowntimeRepository;
import com.aeroplan.scheduling.service.ProposalService;

import jakarta.validation.Valid;
import tools.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/proposals")
public class ProposalController {
    private final ProposalService proposalService;
    private final MachineDowntimeRepository machineDowntimeRepository;
    private final ObjectMapper objectMapper;

    public ProposalController(ProposalService proposalService, MachineDowntimeRepository machineDowntimeRepository,
            ObjectMapper objectMapper) {
        this.proposalService = proposalService;
        this.machineDowntimeRepository = machineDowntimeRepository;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public List<ProposalResponse> list(@RequestParam(required = false) ProposalStatus status) {
        return proposalService.list(status).stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public ProposalResponse get(@PathVariable Long id) {
        return toResponse(proposalService.get(id));
    }

    @PostMapping("/{id}/approve")
    public ProposalResponse approve(@PathVariable Long id, @Valid @RequestBody ApproveRequest request) {
        return toResponse(proposalService.approve(id, request.approvedBy()));
    }

    @PostMapping("/{id}/reject")
    public ProposalResponse reject(@PathVariable Long id, @Valid @RequestBody RejectRequest request) {
        return toResponse(proposalService.reject(id, request.rejectedBy()));
    }

    @SuppressWarnings("unchecked")
    private ProposalResponse toResponse(ScheduleProposal proposal) {
        MachineDowntime downtime = proposal.machineDowntimeId() == null ? null
                : machineDowntimeRepository.findById(proposal.machineDowntimeId()).orElse(null);
        Map<String, Object> findings;
        try {
            findings = objectMapper.readValue(proposal.agentFindings(), Map.class);
        } catch (Exception e) {
            findings = Map.of();
        }
        return ProposalResponse.from(proposal, downtime, findings);
    }
}
