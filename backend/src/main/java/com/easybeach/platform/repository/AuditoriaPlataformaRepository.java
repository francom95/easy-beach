package com.easybeach.platform.repository;

import com.easybeach.platform.domain.AuditoriaPlataforma;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditoriaPlataformaRepository extends JpaRepository<AuditoriaPlataforma, Long> {

    /** Super Admin, cross-tenant intencional (sin filtro de tenant habilitado). */
    Page<AuditoriaPlataforma> findByBalnearioId(Long balnearioId, Pageable pageable);
}
