package com.eventixx.eventcatalog.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eventixx.eventcatalog.dto.venue.CreateVenueRequest;
import com.eventixx.eventcatalog.dto.venue.UpdateVenueRequest;
import com.eventixx.eventcatalog.dto.venue.VenueResponse;
import com.eventixx.eventcatalog.dto.venue.VenueSummaryResponse;
import com.eventixx.eventcatalog.entities.Venue;
import com.eventixx.eventcatalog.exceptions.ResourceNotFoundException;
import com.eventixx.eventcatalog.repositories.VenueRepository;
import com.eventixx.eventcatalog.services.venue.VenueMapper;
import com.eventixx.eventcatalog.services.venue.VenueService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class VenueServiceTest {

  @Mock private VenueRepository venueRepository;
  @Mock private VenueMapper venueMapper;

  @InjectMocks private VenueService venueService;

  private Venue venue;
  private VenueResponse venueResponse;

  @BeforeEach
  void setUp() {
    venue =
        Venue.builder()
            .id(UUID.randomUUID())
            .name("Test Venue")
            .address("123 Main St")
            .city("Sao Paulo")
            .country("Brazil")
            .capacity(100)
            .build();

    venueResponse =
        new VenueResponse(
            venue.getId(),
            venue.getName(),
            venue.getAddress(),
            venue.getCity(),
            venue.getCountry(),
            venue.getCapacity(),
            Instant.now(),
            Instant.now());
  }

  @Test
  void shouldCreateVenue() {
    CreateVenueRequest request = new CreateVenueRequest("New Venue", "Addr", "City", "BR", 200);
    Venue newVenue =
        Venue.builder()
            .name("New Venue")
            .address("Addr")
            .city("City")
            .country("BR")
            .capacity(200)
            .build();

    when(venueMapper.toEntity(request)).thenReturn(newVenue);
    when(venueRepository.save(newVenue)).thenReturn(newVenue);
    when(venueMapper.toResponse(newVenue)).thenReturn(venueResponse);

    VenueResponse result = venueService.create(request);

    assertThat(result).isEqualTo(venueResponse);
  }

  @Test
  void shouldFindVenueById() {
    when(venueRepository.findById(venue.getId())).thenReturn(Optional.of(venue));
    when(venueMapper.toResponse(venue)).thenReturn(venueResponse);

    VenueResponse result = venueService.findById(venue.getId());

    assertThat(result).isEqualTo(venueResponse);
  }

  @Test
  void shouldThrow_whenVenueNotFound() {
    UUID id = UUID.randomUUID();
    when(venueRepository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> venueService.findById(id))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Venue with id");
  }

  @Test
  void shouldFindAllVenues() {
    Pageable pageable = PageRequest.of(0, 10);
    Page<Venue> page = new PageImpl<>(List.of(venue));
    when(venueRepository.findAll(pageable)).thenReturn(page);
    when(venueMapper.toSummary(any(Venue.class)))
        .thenReturn(
            new VenueSummaryResponse(
                venue.getId(), venue.getName(), venue.getCity(), venue.getCountry()));

    Page<VenueSummaryResponse> result = venueService.findAll(pageable);

    assertThat(result.getContent()).hasSize(1);
  }

  @Test
  void shouldUpdateVenue() {
    UpdateVenueRequest request = new UpdateVenueRequest("Updated Venue", null, null, null, null);

    when(venueRepository.findById(venue.getId())).thenReturn(Optional.of(venue));
    when(venueMapper.toResponse(venue)).thenReturn(venueResponse);

    VenueResponse result = venueService.update(venue.getId(), request);

    assertThat(result).isEqualTo(venueResponse);
    verify(venueMapper).updateEntity(request, venue);
  }

  @Test
  void shouldDeleteVenue() {
    when(venueRepository.findById(venue.getId())).thenReturn(Optional.of(venue));

    venueService.delete(venue.getId());

    verify(venueRepository).delete(venue);
  }
}
