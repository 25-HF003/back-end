package com.deeptruth.deeptruth.controller;

import com.deeptruth.deeptruth.service.NoiseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/noise")
public class NoiseController {

    private final NoiseService noiseService;
}