package com.finassistmini.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Finassist Mini API")
                        .version("0.1.0")
                        .description("RAG assistant APIs for ingesting documents and conversational queries")
                        .contact(new Contact().name("Finassist Mini").email("support@example.com")));
    }
}