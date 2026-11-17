package kr.jeommechurial.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "food_catalog")
public class FoodCatalogEntity {
    @Id
    @Column(length = 64)
    private String slug;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(name = "character_id", nullable = false, length = 32)
    private String characterId;

    @Column(name = "image_path", nullable = false, length = 200)
    private String imagePath;

    @Column(name = "selection_basis", nullable = false, length = 32)
    private String selectionBasis;

    protected FoodCatalogEntity() {}

    public String getSlug() { return slug; }
    public int getSortOrder() { return sortOrder; }
    public String getName() { return name; }
    public String getCharacterId() { return characterId; }
    public String getImagePath() { return imagePath; }
    public String getSelectionBasis() { return selectionBasis; }
}
