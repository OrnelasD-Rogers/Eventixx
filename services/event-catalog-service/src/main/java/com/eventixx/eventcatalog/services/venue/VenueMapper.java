package com.eventixx.eventcatalog.services.venue;

import com.eventixx.eventcatalog.dto.venue.CreateVenueRequest;
import com.eventixx.eventcatalog.dto.venue.UpdateVenueRequest;
import com.eventixx.eventcatalog.dto.venue.VenueResponse;
import com.eventixx.eventcatalog.dto.venue.VenueSummaryResponse;

import com.eventixx.eventcatalog.entities.Venue;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface VenueMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    Venue toEntity(CreateVenueRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void updateEntity(UpdateVenueRequest request, @MappingTarget Venue venue);

    VenueResponse toResponse(Venue venue);

    VenueSummaryResponse toSummary(Venue venue);

    List<VenueSummaryResponse> toSummaryList(List<Venue> venues);
}
