package com.easybeach.promotions.repository;

import com.easybeach.promotions.domain.PromocionComboItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromocionComboItemRepository extends JpaRepository<PromocionComboItem, Long> {

    List<PromocionComboItem> findByPromocionId(Long promocionId);

    List<PromocionComboItem> findByPromocionIdIn(List<Long> promocionIds);
}
