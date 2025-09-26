package searchengine.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppConfigProperties {
    private String userAgent;
    private String referrer;
}
