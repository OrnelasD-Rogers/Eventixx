package com.eventixx.eventcatalog.services.venue;

import com.eventixx.eventcatalog.dto.venue.CreateVenueRequest;
import com.eventixx.eventcatalog.dto.venue.UpdateVenueRequest;
import com.eventixx.eventcatalog.dto.venue.VenueResponse;
import com.eventixx.eventcatalog.dto.venue.VenueSummaryResponse;

import com.eventixx.eventcatalog.exceptions.BusinessException;
import com.eventixx.eventcatalog.entities.Venue;
import com.eventixx.eventcatalog.repositories.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VenueService {

    private final VenueRepository venueRepository;
    private final VenueMapper venueMapper;

    @Transactional
    public VenueResponse create(CreateVenueRequest request) {
        Venue venue = venueMapper.toEntity(request);
        return venueMapper.toResponse(venueRepository.save(venue));
    }

    public VenueResponse findById(UUID id) {
        return venueMapper.toResponse(findVenueOrThrow(id));
    }

    public Page<VenueSummaryResponse> findAll(Pageable pageable) {
        return venueRepository.findAll(pageable).map(venueMapper::toSummary);
    }

    @Transactional
    public VenueResponse update(UUID id, UpdateVenueRequest request) {
        Venue venue = findVenueOrThrow(id);
        venueMapper.updateEntity(request, venue);
        return venueMapper.toResponse(venue);
    }

    @Transactional
    public void delete(UUID id) {
        Venue venue = findVenueOrThrow(id);
        venueRepository.delete(venue);
    }

    private Venue findVenueOrThrow(UUID id) {
        return venueRepository.findById(id)
            .orElseThrow(() -> new BusinessException("Venue with id " + id + " not found", HttpStatus.NOT_FOUND));
    }
}
