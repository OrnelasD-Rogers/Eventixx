package com.eventixx.eventcatalog.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/** Enables JPA auditing for @LastModifiedDate support. */
@Configuration
@EnableJpaAuditing
public class PersistenceConfig {}
