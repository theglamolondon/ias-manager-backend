package net.ivoireautoservice.ias_manager.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ias.media")
@Getter
@Setter
public class MediaProperties {
    private String uploadDir;
}
