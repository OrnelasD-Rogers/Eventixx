package com.eventixx.eventcatalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class EventCatalogServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventCatalogServiceApplication.class, args);
    }
}
