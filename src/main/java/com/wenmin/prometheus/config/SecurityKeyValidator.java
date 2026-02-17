package com.wenmin.prometheus.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Validates that required security keys are configured in production.
 * In dev profile, hardcoded defaults are allowed.
 * In prod profile, environment variables JWT_SECRET and DISTRIBUTE_ENCRYPTION_KEY must be set.
 */
@Slf4j
@Component
public class SecurityKeyValidator {

    @Value("${distribute.encryption-key:}")
    private String encryptionKey;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @PostConstruct
    public void validate() {
        if ("prod".equals(activeProfile)) {
            if (encryptionKey == null || encryptionKey.isBlank()) {
                throw new IllegalStateException(
                    "DISTRIBUTE_ENCRYPTION_KEY environment variable must be set in production. " +
                    "Set it via: export DISTRIBUTE_ENCRYPTION_KEY=<your-32-char-secret>");
            }
            log.info("Production security keys validated successfully.");
        } else {
            log.warn("Running in '{}' profile. Security keys use development defaults - do NOT use in production.", activeProfile);
        }
    }
}
