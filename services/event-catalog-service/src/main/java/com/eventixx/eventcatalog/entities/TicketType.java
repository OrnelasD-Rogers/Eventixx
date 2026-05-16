package com.eventixx.eventcatalog.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "ticket_types")
@SQLRestriction("deleted_at IS NULL")
@SQLDelete(
    sql =
        "UPDATE ticket_types SET deleted_at = CURRENT_TIMESTAMP, version = version + 1 WHERE id = ?"
            + " AND version = ?")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@SuppressWarnings("PMD.TooManyFields")
public class TicketType {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Setter
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "event_id", nullable = false)
  private Event event;

  @Setter
  @Column(nullable = false, length = 100)
  private String name;

  @Setter
  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal price;

  @Setter
  @Column(name = "quantity_available", nullable = false)
  private Integer quantityAvailable;

  // CPD-OFF
  @Column(name = "created_at", nullable = false, updatable = false)
  @Builder.Default
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  @Builder.Default
  private Instant updatedAt = Instant.now();

  @Column(name = "deleted_at")
  private Instant deletedAt;

  @Column(name = "deleted_by")
  private String deletedBy;

  @Column(name = "deleted_reason", length = 500)
  private String deletedReason;

  @Version
  @Column(name = "version", nullable = false)
  private long version;
  // CPD-ON
}
