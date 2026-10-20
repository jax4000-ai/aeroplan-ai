package com.aeroplan.workorder.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aeroplan.workorder.repository.ResourceRepository;

@RestController
@RequestMapping("/api/resources")
public class ResourceController {
    private final ResourceRepository resourceRepository;

    public ResourceController(ResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    @GetMapping
    public List<ResourceResponse> list() {
        return resourceRepository.findAll().stream().map(ResourceResponse::from).toList();
    }
}
