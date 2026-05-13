package com.eventixx.searchservice.unit;

import com.eventixx.searchservice.repositories.EventDocument;
import com.eventixx.searchservice.repositories.EventDocumentRepository;
import com.eventixx.searchservice.services.EventDocumentMapper;
import com.eventixx.searchservice.services.EventIndexService;
import com.eventixx.searchservice.services.messaging.EventPublished;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventIndexServiceTest {

    @Mock
    private EventDocumentRepository repository;

    @Mock
    private EventDocumentMapper mapper;

    @InjectMocks
    private EventIndexService eventIndexService;

    @Captor
    private ArgumentCaptor<EventDocument> documentCaptor;

    @Test
    void shouldIndexEvent() {
        var event = createSampleEvent();
        var doc = EventDocument.builder()
                .eventId(event.eventId().toString())
                .title(event.title())
                .build();

        when(mapper.toDocument(event)).thenReturn(doc);

        eventIndexService.indexEvent(event);

        verify(repository).save(documentCaptor.capture());
        assertThat(documentCaptor.getValue().getEventId()).isEqualTo(event.eventId().toString());
        assertThat(documentCaptor.getValue().getTitle()).isEqualTo(event.title());
    }

    private EventPublished createSampleEvent() {
        return new EventPublished(
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
                List.of(),
                Instant.now(),
                UUID.randomUUID(),
                UUID.randomUUID()
        );
    }
}
