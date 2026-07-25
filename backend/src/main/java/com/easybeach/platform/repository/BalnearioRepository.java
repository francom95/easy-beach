package com.easybeach.platform.repository;

import com.easybeach.platform.domain.Balneario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BalnearioRepository extends JpaRepository<Balneario, Long> {

    Optional<Balneario> findBySlug(String slug);
}
