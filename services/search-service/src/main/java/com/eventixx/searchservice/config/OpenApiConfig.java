package com.eventixx.searchservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    /** OpenAPI definition for the Search Service. */
    @Bean
    public OpenAPI searchServiceApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Search Service API")
                        .description("Event search and discovery API")
                        .version("1.0.0"));
    }
}
