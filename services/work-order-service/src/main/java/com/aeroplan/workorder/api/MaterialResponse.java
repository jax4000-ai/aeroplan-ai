package com.aeroplan.workorder.api;

import com.aeroplan.workorder.domain.Material;

public record MaterialResponse(
        Long id,
        String partNumber,
        String description,
        int quantityOnHand,
        int quantityOnOrder,
        String supplier,
        String expectedAt,
        String status) {

    public static MaterialResponse from(Material material) {
        return new MaterialResponse(
                material.id(),
                material.partNumber(),
                material.description(),
                material.quantityOnHand(),
                material.quantityOnOrder(),
                material.supplier(),
                material.expectedAt() == null ? null : material.expectedAt().toString(),
                material.status().name());
    }
}
