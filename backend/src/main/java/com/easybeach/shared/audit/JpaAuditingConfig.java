package com.easybeach.shared.audit;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "utcDateTimeProvider")
public class JpaAuditingConfig {

    @Bean
    public org.springframework.data.auditing.DateTimeProvider utcDateTimeProvider() {
        return () -> java.util.Optional.of(Clock.systemUTC().instant());
    }
}
