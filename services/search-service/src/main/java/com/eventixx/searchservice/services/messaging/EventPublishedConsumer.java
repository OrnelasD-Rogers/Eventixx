package com.eventixx.searchservice.services.messaging;

import com.eventixx.searchservice.services.EventIndexService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventPublishedConsumer {

  private final EventIndexService eventIndexService;
  private final ObjectMapper objectMapper;

  /** Consumes event.published messages from Kafka and indexes them in Elasticsearch. */
  @KafkaListener(topics = "event.published", groupId = "${spring.kafka.consumer.group-id}")
  @SuppressWarnings("PMD.AvoidCatchingGenericException")
  public void consume(String message) {
    try {
      EventPublished event = objectMapper.readValue(message, EventPublished.class);
      log.info("Received event.published: eventId={}", event.eventId());
      eventIndexService.indexEvent(event);
    } catch (Exception e) {
      log.error("Failed to process event.published message: {}", e.getMessage(), e);
    }
  }
}
