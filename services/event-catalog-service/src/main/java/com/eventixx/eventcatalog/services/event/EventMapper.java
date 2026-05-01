package com.eventixx.eventcatalog.services.event;

import com.eventixx.eventcatalog.dto.event.CreateEventRequest;
import com.eventixx.eventcatalog.dto.event.EventResponse;
import com.eventixx.eventcatalog.dto.event.EventSummaryResponse;
import com.eventixx.eventcatalog.dto.event.UpdateEventRequest;

import com.eventixx.eventcatalog.services.tickettype.TicketTypeMapper;
import com.eventixx.eventcatalog.entities.Event;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = TicketTypeMapper.class)
public interface EventMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", constant = "DRAFT")
    @Mapping(target = "venue", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "ticketTypes", ignore = true)
    @Mapping(target = "publishedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    Event toEntity(CreateEventRequest request);

    @Mapping(target = "venue", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "ticketTypes", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "publishedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void updateEntity(UpdateEventRequest request, @MappingTarget Event event);

    @Mapping(source = "venue.id", target = "venueId")
    @Mapping(source = "venue.name", target = "venueName")
    @Mapping(source = "category.id", target = "categoryId")
    @Mapping(source = "category.name", target = "categoryName")
    EventResponse toResponse(Event event);

    @Mapping(source = "venue.name", target = "venueName")
    @Mapping(source = "category.name", target = "categoryName")
    EventSummaryResponse toSummary(Event event);

    List<EventSummaryResponse> toSummaryList(List<Event> events);
}
