package com.finassistmini.web;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check the health status of the API")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }
}
