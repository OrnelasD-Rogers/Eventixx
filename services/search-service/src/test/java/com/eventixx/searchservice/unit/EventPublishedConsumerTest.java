package com.eventixx.searchservice.unit;

import com.eventixx.searchservice.services.EventIndexService;
import com.eventixx.searchservice.services.messaging.EventPublished;
import com.eventixx.searchservice.services.messaging.EventPublishedConsumer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EventPublishedConsumerTest {

    @Mock
    private EventIndexService eventIndexService;

    private EventPublishedConsumer consumer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        consumer = new EventPublishedConsumer(eventIndexService, objectMapper);
    }

    @Test
    void shouldConsumeAndIndexEvent() throws Exception {
        var event = new EventPublished(
                UUID.randomUUID(),
                "Test Event",
                "Description",
                UUID.randomUUID(),
                "Music",
                UUID.randomUUID(),
                "Venue",
                "Sao Paulo",
                "Brazil",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Instant.now(),
                List.of(new EventPublished.TicketTypeInfo("VIP", BigDecimal.valueOf(100), 50)),
                Instant.now(),
                UUID.randomUUID(),
                UUID.randomUUID()
        );

        String json = objectMapper.writeValueAsString(event);

        consumer.consume(json);

        verify(eventIndexService).indexEvent(event);
    }
}
