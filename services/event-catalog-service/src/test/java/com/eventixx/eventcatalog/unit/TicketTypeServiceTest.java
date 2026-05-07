package com.eventixx.eventcatalog.unit;

import com.eventixx.eventcatalog.dto.tickettype.CreateTicketTypeRequest;
import com.eventixx.eventcatalog.dto.tickettype.TicketTypeResponse;
import com.eventixx.eventcatalog.dto.tickettype.UpdateTicketTypeRequest;
import com.eventixx.eventcatalog.entities.Event;
import com.eventixx.eventcatalog.entities.TicketType;
import com.eventixx.eventcatalog.exceptions.BusinessException;
import com.eventixx.eventcatalog.repositories.EventRepository;
import com.eventixx.eventcatalog.repositories.TicketTypeRepository;
import com.eventixx.eventcatalog.services.tickettype.TicketTypeMapper;
import com.eventixx.eventcatalog.services.tickettype.TicketTypeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketTypeServiceTest {

    @Mock
    private TicketTypeRepository ticketTypeRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private TicketTypeMapper ticketTypeMapper;

    @InjectMocks
    private TicketTypeService ticketTypeService;

    private Event event;
    private TicketType ticketType;
    private TicketTypeResponse ticketTypeResponse;

    @BeforeEach
    void setUp() {
        event = Event.builder()
            .id(UUID.randomUUID())
            .title("Test Event")
            .build();

        ticketType = TicketType.builder()
            .id(UUID.randomUUID())
            .name("GA")
            .price(BigDecimal.valueOf(50))
            .quantityAvailable(100)
            .event(event)
            .build();

        ticketTypeResponse = new TicketTypeResponse(
            ticketType.getId(), ticketType.getName(), ticketType.getPrice(),
            ticketType.getQuantityAvailable(), Instant.now(), Instant.now()
        );
    }

    @Test
    void shouldCreateTicketType_forEvent() {
        CreateTicketTypeRequest request = new CreateTicketTypeRequest("VIP", BigDecimal.valueOf(150), 50);
        TicketType newTicketType = TicketType.builder().name("VIP").price(BigDecimal.valueOf(150)).quantityAvailable(50).build();

        when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
        when(ticketTypeMapper.toEntity(request)).thenReturn(newTicketType);
        when(ticketTypeRepository.save(newTicketType)).thenReturn(newTicketType);
        when(ticketTypeMapper.toResponse(newTicketType)).thenReturn(ticketTypeResponse);

        TicketTypeResponse result = ticketTypeService.create(event.getId(), request);

        assertThat(result).isEqualTo(ticketTypeResponse);
        assertThat(newTicketType.getEvent()).isEqualTo(event);
    }

    @Test
    void shouldThrow_whenEventNotFound_onCreate() {
        CreateTicketTypeRequest request = new CreateTicketTypeRequest("VIP", BigDecimal.valueOf(150), 50);
        when(eventRepository.findById(event.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketTypeService.create(event.getId(), request))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("Event with id")
            .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void shouldFindTicketTypeById() {
        when(ticketTypeRepository.findById(ticketType.getId())).thenReturn(Optional.of(ticketType));
        when(ticketTypeMapper.toResponse(ticketType)).thenReturn(ticketTypeResponse);

        TicketTypeResponse result = ticketTypeService.findById(ticketType.getId());

        assertThat(result).isEqualTo(ticketTypeResponse);
    }

    @Test
    void shouldThrow_whenTicketTypeNotFound() {
        UUID id = UUID.randomUUID();
        when(ticketTypeRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketTypeService.findById(id))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("Ticket type with id")
            .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void shouldFindAllByEventId() {
        when(ticketTypeRepository.findAllByEventId(event.getId())).thenReturn(List.of(ticketType));
        when(ticketTypeMapper.toResponseList(List.of(ticketType))).thenReturn(List.of(ticketTypeResponse));

        List<TicketTypeResponse> result = ticketTypeService.findAllByEventId(event.getId());

        assertThat(result).hasSize(1);
    }

    @Test
    void shouldUpdateTicketType() {
        UpdateTicketTypeRequest request = new UpdateTicketTypeRequest("Updated GA", null, null);

        when(ticketTypeRepository.findById(ticketType.getId())).thenReturn(Optional.of(ticketType));
        when(ticketTypeMapper.toResponse(ticketType)).thenReturn(ticketTypeResponse);

        TicketTypeResponse result = ticketTypeService.update(ticketType.getId(), request);

        assertThat(result).isEqualTo(ticketTypeResponse);
        verify(ticketTypeMapper).updateEntity(request, ticketType);
    }

    @Test
    void shouldDeleteTicketType() {
        when(ticketTypeRepository.findById(ticketType.getId())).thenReturn(Optional.of(ticketType));

        ticketTypeService.delete(ticketType.getId());

        verify(ticketTypeRepository).delete(ticketType);
    }
}
