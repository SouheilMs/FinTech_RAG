package com.finassistmini.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Manually registers Swagger UI static resources to fix the issue where springdoc's
 * SwaggerUiWebMvcConfigurer auto-configuration does not load properly.
 *
 * This ensures that /swagger-ui/** requests are correctly routed to the swagger-ui
 * webjar resources in the classpath.
 */
@Configuration
public class SwaggerUiWebMvcConfigurer implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/swagger-ui/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/swagger-ui/")
                .setCachePeriod(0);

        registry.addResourceHandler("/swagger-ui.html")
                .addResourceLocations("classpath:/META-INF/resources/")
                .setCachePeriod(0);

        registry.addResourceHandler("/v3/api-docs/**")
                .addResourceLocations("classpath:/META-INF/resources/")
                .setCachePeriod(0);
    }
}
