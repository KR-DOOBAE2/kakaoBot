package kr.jeommechurial.app;

import java.util.List;
import kr.jeommechurial.persistence.FoodCatalogEntity;
import kr.jeommechurial.persistence.FoodCatalogJpa;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FoodCatalogController {
    private final FoodCatalogJpa foods;

    public FoodCatalogController(FoodCatalogJpa foods) { this.foods = foods; }

    @GetMapping("/api/foods")
    public List<FoodItem> list() {
        return foods.findAllByOrderBySortOrderAsc().stream().map(FoodItem::from).toList();
    }

    public record FoodItem(String slug, String name, int sortOrder, String characterId, String imagePath) {
        static FoodItem from(FoodCatalogEntity food) {
            return new FoodItem(food.getSlug(), food.getName(), food.getSortOrder(), food.getCharacterId(), food.getImagePath());
        }
    }
}
