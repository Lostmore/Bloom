package app.bloom.gateway.docs;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.ResourceHandlerRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@Configuration
@EnableConfigurationProperties(ApiDocsProperties.class)
@ConditionalOnProperty(name = "bloom.api-docs.enabled", havingValue = "true", matchIfMissing = true)
public class ApiDocsConfiguration implements WebFluxConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/docs/assets/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/swagger-ui/5.32.15/");
        registry.addResourceHandler("/docs/**").addResourceLocations("classpath:/api-docs-ui/");
    }
}
