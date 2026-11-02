package com.moustapha.healthhub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class HealthHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(HealthHubApplication.class, args);
    }

}
