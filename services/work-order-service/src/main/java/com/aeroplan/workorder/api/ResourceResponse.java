package com.aeroplan.workorder.api;

import com.aeroplan.workorder.domain.Resource;

public record ResourceResponse(
        Long id,
        String name,
        String resourceType,
        java.util.List<String> skills,
        int capacity,
        String availabilityStatus) {

    public static ResourceResponse from(Resource resource) {
        return new ResourceResponse(
                resource.id(), resource.name(), resource.resourceType(), resource.skills(), resource.capacity(), resource.availabilityStatus().name());
    }
}
