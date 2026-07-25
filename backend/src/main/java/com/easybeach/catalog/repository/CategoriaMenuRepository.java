package com.easybeach.catalog.repository;

import com.easybeach.catalog.domain.CategoriaMenu;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaMenuRepository extends JpaRepository<CategoriaMenu, Long> {

    List<CategoriaMenu> findByBalnearioIdOrderByOrdenAsc(Long balnearioId);

    List<CategoriaMenu> findByBalnearioIdAndActivaTrueOrderByOrdenAsc(Long balnearioId);
}
