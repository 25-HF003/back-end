package com.deeptruth.deeptruth.controller;

import com.deeptruth.deeptruth.base.dto.noise.NoiseDTO;
import com.deeptruth.deeptruth.service.NoiseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/noise")
@RequiredArgsConstructor
public class NoiseController {

    private final NoiseService noiseService;

    @GetMapping
    public ResponseEntity<List<NoiseDTO>> getAllNoiseRecordsByUserId(@RequestParam Long userId) {
        List<NoiseDTO> noiseRecords = noiseService.getAllNoiseRecords(userId);
        return ResponseEntity.ok(noiseRecords);
    }
}