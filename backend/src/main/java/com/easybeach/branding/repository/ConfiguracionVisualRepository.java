package com.easybeach.branding.repository;

import com.easybeach.branding.domain.ConfiguracionVisual;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfiguracionVisualRepository extends JpaRepository<ConfiguracionVisual, Long> {

    Optional<ConfiguracionVisual> findByBalnearioId(Long balnearioId);
}
