package com.eventixx.eventcatalog.unit;

import com.eventixx.eventcatalog.services.DomainEvent;
import com.eventixx.eventcatalog.services.messaging.KafkaDomainEventPublisher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaDomainEventPublisherTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private KafkaDomainEventPublisher publisher;

    private TestDomainEvent event;

    @BeforeEach
    void setUp() {
        event = new TestDomainEvent();
    }

    @Test
    void shouldPublishEvent_toCorrectTopicAndKey() throws JsonProcessingException {
        String payload = "{\"type\":\"test\"}";
        when(objectMapper.writeValueAsString(event)).thenReturn(payload);

        publisher.publish(event);

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);

        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), payloadCaptor.capture());

        assertThat(topicCaptor.getValue()).isEqualTo("test.event");
        assertThat(keyCaptor.getValue()).isEqualTo(event.getAggregateId().toString());
        assertThat(payloadCaptor.getValue()).isEqualTo(payload);
    }

    @Test
    void shouldThrow_whenSerializationFails() throws JsonProcessingException {
        when(objectMapper.writeValueAsString(event)).thenThrow(new JsonProcessingException("Boom") {});

        assertThatThrownBy(() -> publisher.publish(event))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Failed to serialize domain event");
    }

    // Test double
    static class TestDomainEvent implements DomainEvent {
        private final UUID aggregateId = UUID.randomUUID();
        private final UUID eventId = UUID.randomUUID();
        private final UUID correlationId = UUID.randomUUID();
        private final Instant timestamp = Instant.now();

        @Override
        public UUID getEventId() { return eventId; }

        @Override
        public String getEventType() { return "test.event"; }

        @Override
        public String getEventVersion() { return "1.0"; }

        @Override
        public Instant getTimestamp() { return timestamp; }

        @Override
        public UUID getAggregateId() { return aggregateId; }

        @Override
        public UUID getCorrelationId() { return correlationId; }
    }
}
