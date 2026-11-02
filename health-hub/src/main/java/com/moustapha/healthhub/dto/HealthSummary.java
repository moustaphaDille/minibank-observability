package com.moustapha.healthhub.dto;
import java.time.Instant;
import java.util.List;

public record HealthSummary(String status, Instant checkedAt, List<ServiceHealth> services) {

}
