package com.moustapha.healthhub.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "supervision")
public record SupervisionProperties(List<Target> services) {

    public record Target(String name, String healthUrl) {}
}
