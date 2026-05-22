package com.eventixx.eventcatalog.services;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Encapsulates Micrometer metric operations for the event catalog domain.
 *
 * <p>Provides counters for key business events ({@code event.created}, {@code event.published}).
 * Using a separate component keeps Micrometer complexity isolated and makes testing easier
 * (services can mock this component instead of mocking MeterRegistry directly).
 */
@Component
@RequiredArgsConstructor
public class EventMetricsService {

  private final MeterRegistry meterRegistry;

  private Counter eventCreatedCounter;
  private Counter eventPublishedCounter;

  /** Initializes Micrometer counters. Called after dependency injection is complete. */
  @PostConstruct
  void init() {
    eventCreatedCounter =
        Counter.builder("event.created")
            .description("Total number of events created")
            .tag("service", "event-catalog")
            .register(meterRegistry);

    eventPublishedCounter =
        Counter.builder("event.published")
            .description("Total number of events published")
            .tag("service", "event-catalog")
            .register(meterRegistry);
  }

  /** Increments the {@code event.created} counter. */
  public void recordEventCreated() {
    eventCreatedCounter.increment();
  }

  /** Increments the {@code event.published} counter. */
  public void recordEventPublished() {
    eventPublishedCounter.increment();
  }
}
