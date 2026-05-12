package com.eventixx.searchservice.config;

import org.mapstruct.MapperConfig;
import org.mapstruct.ReportingPolicy;

/** Central MapStruct configuration for the Search Service. */
@MapperConfig(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MapStructConfig {
}
