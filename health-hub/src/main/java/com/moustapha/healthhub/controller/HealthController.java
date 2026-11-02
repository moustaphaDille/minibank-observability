package com.moustapha.healthhub.controller;
import com.moustapha.healthhub.dto.HealthSummary;
import com.moustapha.healthhub.service.HealthCheckService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final HealthCheckService service;

    public HealthController(HealthCheckService service) {
        this.service = service;
    }

    @GetMapping("/summary")
    public HealthSummary summary() {
        return service.check();
    }
}
