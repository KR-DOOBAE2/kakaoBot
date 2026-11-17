package kr.jeommechurial.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FoodCatalogJpa extends JpaRepository<FoodCatalogEntity, String> {
    List<FoodCatalogEntity> findAllByOrderBySortOrderAsc();
}
