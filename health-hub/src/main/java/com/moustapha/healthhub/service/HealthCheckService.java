package com.moustapha.healthhub.service;

import com.moustapha.healthhub.config.SupervisionProperties;
import com.moustapha.healthhub.dto.HealthSummary;
import com.moustapha.healthhub.dto.ServiceHealth;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class HealthCheckService {

    private final SupervisionProperties properties;
    private final RestClient restClient;

    public HealthCheckService(SupervisionProperties properties) {
        this.properties = properties;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(1000); // 1 s pour se connecter
        factory.setReadTimeout(2000);    // 2 s pour lire la réponse
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    public HealthSummary check() {
        List<ServiceHealth> results = properties.services().stream()
                .map(this::checkOne)
                .toList();
        return new HealthSummary(globalStatus(results), Instant.now(), results);
    }

    private ServiceHealth checkOne(SupervisionProperties.Target target) {
        long start = System.nanoTime();
        try {
            Map<?, ?> body = restClient.get()
                    .uri(target.healthUrl())
                    .retrieve()
                    .body(Map.class);
            long ms = (System.nanoTime() - start) / 1_000_000;
            String status = body != null && body.get("status") != null
                    ? body.get("status").toString()
                    : "UNKNOWN";
            return new ServiceHealth(target.name(), status, ms);
        } catch (Exception e) {
            // connexion refusée, timeout, ou réponse 503 : on considère le service DOWN
            return new ServiceHealth(target.name(), "DOWN", null);
        }
    }

    private String globalStatus(List<ServiceHealth> results) {
        long up = results.stream().filter(s -> "UP".equals(s.status())).count();
        if (up == results.size()) return "UP";
        if (up == 0) return "DOWN";
        return "DEGRADED";
    }
}
