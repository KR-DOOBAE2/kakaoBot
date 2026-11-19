package kr.jeommechurial.app;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GoodPriceCatalogTest {
    private final GoodPriceCatalog catalog = new GoodPriceCatalog();

    @Test void findsPublishedPriceForMatchingRestaurantAndMenu() {
        var found = catalog.find("카츠노미", "서울 강남구 논현로76길 11", "서울 강남구 언주로 725", "로스카츠");
        assertThat(found).isPresent();
        assertThat(found.orElseThrow().won()).isEqualTo(9800);
        assertThat(found.orElseThrow().asOf()).isEqualTo("2026-09-30");
    }

    @Test void doesNotTreatDifferentMenuOrDistrictAsVerified() {
        assertThat(catalog.find("카츠노미", "서울 강남구 논현로76길 11", "서울 강남구 언주로 725", "생선구이"))
                .isEmpty();
        assertThat(catalog.find("카츠노미", "서울 서초구 어느길 11", "서울 강남구 언주로 725", "로스카츠"))
                .isEmpty();
    }

    @Test void readsQuotedCsvCells() {
        assertThat(GoodPriceCatalog.parseCsv("한식,\"가게, 본점\",\"메뉴 \"\"특\"\"\",9000"))
                .containsExactly("한식", "가게, 본점", "메뉴 \"특\"", "9000");
    }
}
