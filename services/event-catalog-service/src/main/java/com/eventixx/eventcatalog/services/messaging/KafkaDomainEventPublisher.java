package com.eventixx.eventcatalog.services.messaging;

import com.eventixx.eventcatalog.exceptions.EventSerializationException;
import com.eventixx.eventcatalog.services.DomainEvent;
import com.eventixx.eventcatalog.services.DomainEventPublisher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaDomainEventPublisher implements DomainEventPublisher {

  private final KafkaTemplate<String, String> kafkaTemplate;
  private final ObjectMapper objectMapper;

  @Override
  public void publish(DomainEvent event) {
    try {
      String payload = objectMapper.writeValueAsString(event);
      String topic = event.getEventType();
      String key = event.getAggregateId().toString();

      log.info(
          "Publishing domain event to topic {}: type={}, aggregateId={}",
          topic,
          event.getEventType(),
          key);
      kafkaTemplate.send(topic, key, payload);
    } catch (JsonProcessingException e) {
      throw new EventSerializationException(
          "Failed to serialize domain event: " + event.getEventType(), e);
    }
  }
}
