package com.eventixx.eventcatalog.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
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
@Table(name = "venues")
@SQLRestriction("deleted_at IS NULL")
@SQLDelete(
    sql =
        "UPDATE venues SET deleted_at = CURRENT_TIMESTAMP, version = version + 1 WHERE id = ?"
            + " AND version = ?")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@SuppressWarnings("PMD.TooManyFields")
public class Venue {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Setter
  @Column(nullable = false, length = 200)
  private String name;

  @Setter
  @Column(nullable = false, length = 500)
  private String address;

  @Setter
  @Column(nullable = false, length = 100)
  private String city;

  @Setter
  @Column(nullable = false, length = 100)
  private String country;

  @Setter
  @Column(nullable = false)
  private Integer capacity;

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
