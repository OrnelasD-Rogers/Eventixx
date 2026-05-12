package com.eventixx.searchservice.repositories;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Elasticsearch document representing an indexed event. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "events", createIndex = true)
public class EventDocument {

    @Id
    private String eventId;

    @Field(type = FieldType.Text)
    private String title;

    @Field(type = FieldType.Text)
    private String description;

    @Field(type = FieldType.Keyword)
    private String categoryName;

    @Field(type = FieldType.Text)
    private String venueName;

    @Field(type = FieldType.Keyword)
    private String city;

    @Field(type = FieldType.Keyword)
    private String country;

    @Field(type = FieldType.Date)
    private Instant startTime;

    @Field(type = FieldType.Date)
    private Instant endTime;

    @Field(type = FieldType.Date)
    private Instant publishedAt;

    @Field(type = FieldType.Scaled_Float, scalingFactor = 100)
    private BigDecimal minPrice;

    @Field(type = FieldType.Scaled_Float, scalingFactor = 100)
    private BigDecimal maxPrice;

    @Field(type = FieldType.Nested)
    private List<TicketTypeDocument> ticketTypes;

    /** Nested document for ticket type within an event. */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TicketTypeDocument {
        private String name;
        @Field(type = FieldType.Scaled_Float, scalingFactor = 100)
        private BigDecimal price;
        private Integer quantityAvailable;
    }
}
