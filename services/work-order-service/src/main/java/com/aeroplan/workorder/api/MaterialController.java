package com.aeroplan.workorder.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aeroplan.workorder.domain.Material;
import com.aeroplan.workorder.domain.MaterialStatus;
import com.aeroplan.workorder.repository.MaterialRepository;

@RestController
@RequestMapping("/api/materials")
public class MaterialController {
    private final MaterialRepository materialRepository;

    public MaterialController(MaterialRepository materialRepository) {
        this.materialRepository = materialRepository;
    }

    @GetMapping
    public List<MaterialResponse> list() {
        return materialRepository.findAll().stream().map(MaterialResponse::from).toList();
    }

    @GetMapping("/shortages")
    public List<MaterialResponse> shortages() {
        return materialRepository.findAll().stream()
                .filter(m -> m.status() != MaterialStatus.IN_STOCK)
                .map(MaterialResponse::from)
                .toList();
    }
}
