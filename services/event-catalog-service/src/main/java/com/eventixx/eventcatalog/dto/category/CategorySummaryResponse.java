package com.eventixx.eventcatalog.dto.category;

import java.util.UUID;

public record CategorySummaryResponse(
    UUID id,
    String name
) {}
