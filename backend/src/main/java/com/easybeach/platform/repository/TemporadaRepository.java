package com.easybeach.platform.repository;

import com.easybeach.platform.domain.EstadoTemporada;
import com.easybeach.platform.domain.Temporada;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TemporadaRepository extends JpaRepository<Temporada, Long> {

    List<Temporada> findByEstado(EstadoTemporada estado);
}
