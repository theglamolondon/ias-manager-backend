package net.ivoireautoservice.ias_manager.auth;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ias.security")
@Getter
@Setter
public class JwtProperties {
    private String secret;
    private long expiration;
    private long refreshExpiration;
    private String issuer;
    private int loginMaxAttempts;
    private long loginBlockMinutes;
}
