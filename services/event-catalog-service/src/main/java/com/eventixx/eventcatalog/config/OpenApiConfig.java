package com.eventixx.eventcatalog.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  /**
   * Configures OpenAPI documentation bean.
   *
   * @return the OpenAPI configuration
   */
  @Bean
  public OpenAPI eventCatalogOpenAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Eventixx Event Catalog API")
                .description("REST API for managing events, venues, categories, and ticket types")
                .version("v1.0"));
  }
}
