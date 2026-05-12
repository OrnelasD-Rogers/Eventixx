package com.eventixx.searchservice.unit;

import com.eventixx.searchservice.repositories.EventDocument;
import com.eventixx.searchservice.services.EventDocumentMapper;
import com.eventixx.searchservice.services.EventDocumentMapperImpl;
import com.eventixx.searchservice.services.messaging.EventPublished;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EventDocumentMapperTest {

    private EventDocumentMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new EventDocumentMapperImpl();
    }

    @Test
    void shouldMapEventPublishedToDocument() {
        var ticketTypes = List.of(
                new EventPublished.TicketTypeInfo("VIP", BigDecimal.valueOf(100.00), 50),
                new EventPublished.TicketTypeInfo("General", BigDecimal.valueOf(50.00), 200)
        );
        var event = new EventPublished(
                UUID.randomUUID(),
                "Test Event",
                "Description",
                UUID.randomUUID(),
                "Music",
                UUID.randomUUID(),
                "Venue Name",
                "Sao Paulo",
                "Brazil",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Instant.now(),
                ticketTypes,
                Instant.now(),
                UUID.randomUUID(),
                UUID.randomUUID()
        );

        EventDocument doc = mapper.toDocument(event);

        assertThat(doc.getEventId()).isEqualTo(event.eventId().toString());
        assertThat(doc.getTitle()).isEqualTo(event.title());
        assertThat(doc.getDescription()).isEqualTo(event.description());
        assertThat(doc.getCategoryName()).isEqualTo(event.categoryName());
        assertThat(doc.getVenueName()).isEqualTo(event.venueName());
        assertThat(doc.getCity()).isEqualTo(event.city());
        assertThat(doc.getCountry()).isEqualTo(event.country());
        assertThat(doc.getMinPrice()).isEqualByComparingTo(BigDecimal.valueOf(50.00));
        assertThat(doc.getMaxPrice()).isEqualByComparingTo(BigDecimal.valueOf(100.00));
        assertThat(doc.getTicketTypes()).hasSize(2);
    }

    @Test
    void shouldHandleEmptyTicketTypes() {
        var event = new EventPublished(
                UUID.randomUUID(),
                "Event",
                "Desc",
                UUID.randomUUID(),
                "Category",
                UUID.randomUUID(),
                "Venue",
                "City",
                "Country",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Instant.now(),
                List.of(),
                Instant.now(),
                UUID.randomUUID(),
                UUID.randomUUID()
        );

        EventDocument doc = mapper.toDocument(event);

        assertThat(doc.getMinPrice()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(doc.getMaxPrice()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(doc.getTicketTypes()).isEmpty();
    }

    @Test
    void shouldHandleNullTicketTypes() {
        var event = new EventPublished(
                UUID.randomUUID(),
                "Event",
                "Desc",
                UUID.randomUUID(),
                "Category",
                UUID.randomUUID(),
                "Venue",
                "City",
                "Country",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Instant.now(),
                null,
                Instant.now(),
                UUID.randomUUID(),
                UUID.randomUUID()
        );

        EventDocument doc = mapper.toDocument(event);

        assertThat(doc.getMinPrice()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(doc.getMaxPrice()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(doc.getTicketTypes()).isNull();
    }

    @Test
    void shouldMapTicketTypeToDocument() {
        var ticketType = new EventPublished.TicketTypeInfo("VIP", BigDecimal.valueOf(200.00), 100);

        EventDocument.TicketTypeDocument result = mapper.toTicketDocument(ticketType);

        assertThat(result.getName()).isEqualTo("VIP");
        assertThat(result.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(200.00));
        assertThat(result.getQuantityAvailable()).isEqualTo(100);
    }
}
