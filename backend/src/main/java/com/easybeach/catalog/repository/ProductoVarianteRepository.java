package com.easybeach.catalog.repository;

import com.easybeach.catalog.domain.ProductoVariante;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoVarianteRepository extends JpaRepository<ProductoVariante, Long> {

    List<ProductoVariante> findByProductoIdOrderByOrdenAsc(Long productoId);

    List<ProductoVariante> findByProductoIdAndDisponibleTrueOrderByOrdenAsc(Long productoId);

    /** Para el menú público: variantes disponibles de un lote de productos en una sola query. */
    List<ProductoVariante> findByProductoIdInAndDisponibleTrue(List<Long> productoIds);
}
