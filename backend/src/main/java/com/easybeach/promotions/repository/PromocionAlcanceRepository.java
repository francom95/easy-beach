package com.easybeach.promotions.repository;

import com.easybeach.promotions.domain.PromocionAlcance;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromocionAlcanceRepository extends JpaRepository<PromocionAlcance, Long> {

    List<PromocionAlcance> findByPromocionId(Long promocionId);

    List<PromocionAlcance> findByPromocionIdIn(List<Long> promocionIds);
}
