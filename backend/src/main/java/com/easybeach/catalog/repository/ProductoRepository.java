package com.easybeach.catalog.repository;

import com.easybeach.catalog.domain.Producto;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    List<Producto> findByBalnearioIdOrderByOrdenAsc(Long balnearioId);

    List<Producto> findByBalnearioIdAndCategoriaIdAndDisponibleTrueOrderByOrdenAsc(Long balnearioId, Long categoriaId);

    boolean existsByCategoriaId(Long categoriaId);

    List<Producto> findByBalnearioIdAndDisponibleTrueOrderByOrdenAsc(Long balnearioId);
}
