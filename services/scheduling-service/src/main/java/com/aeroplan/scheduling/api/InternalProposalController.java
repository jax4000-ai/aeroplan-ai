package com.aeroplan.scheduling.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.aeroplan.scheduling.service.ProposalService;

import tools.jackson.databind.ObjectMapper;

/**
 * Internal-only endpoints called by the Step Functions HTTP-bridge Lambda, never exposed via API
 * Gateway (see docs/api-specification.md). No auth is enforced at this layer — that's the API
 * Gateway/VPC boundary's job per docs/infrastructure-spec.md.
 */
@RestController
@RequestMapping("/internal/proposals")
public class InternalProposalController {
    private final ProposalService proposalService;
    private final ObjectMapper objectMapper;

    public InternalProposalController(ProposalService proposalService, ObjectMapper objectMapper) {
        this.proposalService = proposalService;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProposalIdResponse create(@RequestBody CreateProposalRequest request) {
        String findingsJson;
        try {
            findingsJson = objectMapper.writeValueAsString(request.agentFindings() == null ? java.util.Map.of() : request.agentFindings());
        } catch (Exception e) {
            findingsJson = "{}";
        }
        Long proposalId = proposalService.create(request.machineDowntimeId(), request.basedOnScheduleVersionId(),
                request.candidateScheduleVersionId(), request.agentSummary(), findingsJson, request.taskToken(),
                request.executionArn());
        return new ProposalIdResponse(proposalId);
    }

    @PostMapping("/expire")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void expire(@RequestBody ExpireRequest request) {
        proposalService.expire(request.executionArn());
    }
}
