package com.aeroplan.workorder.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aeroplan.workorder.service.RocketService;

@RestController
@RequestMapping("/api/rockets")
public class RocketController {
    private final RocketService rocketService;

    public RocketController(RocketService rocketService) {
        this.rocketService = rocketService;
    }

    @GetMapping
    public List<RocketResponse> list() {
        return rocketService.list().stream().map(rocketService::toResponse).toList();
    }

    @GetMapping("/{id}")
    public RocketResponse get(@PathVariable Long id) {
        return rocketService.toResponse(rocketService.get(id));
    }
}
