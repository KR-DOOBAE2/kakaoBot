package kr.jeommechurial.app;

import kr.jeommechurial.service.MenuPriceLookup;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

@Component
public class GoodPriceCatalog implements MenuPriceLookup {
    static final String SOURCE = "행정안전부 착한가격업소";
    static final String AS_OF = "2026-09-30";
    private static final Pattern DISTRICT = Pattern.compile("([가-힣]+구|[가-힣]+군)");
    private final Map<String, List<Entry>> byName;

    public GoodPriceCatalog() {
        try (var input = GoodPriceCatalog.class.getResourceAsStream("/data/goodprice-20260930.csv.gz")) {
            if (input == null) throw new IllegalStateException("착한가격업소 자료가 없습니다.");
            byName = load(new BufferedReader(new InputStreamReader(new GZIPInputStream(input), StandardCharsets.UTF_8)));
        } catch (IOException e) {
            throw new IllegalStateException("착한가격업소 자료를 읽지 못했습니다.", e);
        }
    }

    static Map<String, List<Entry>> load(BufferedReader reader) throws IOException {
        Map<String, List<Entry>> entries = new HashMap<>();
        reader.readLine(); // header
        String line;
        while ((line = reader.readLine()) != null) {
            List<String> cells = parseCsv(line);
            if (cells.size() != 13 || !cells.get(2).contains("식") || cells.get(3).isBlank()) continue;
            String name = normalize(cells.get(3));
            String district = district(cells.get(4));
            if (district.isBlank()) continue;
            for (int i = 5; i <= 11; i += 2) {
                String menu = cells.get(i).strip();
                String digits = cells.get(i + 1).replaceAll("[^0-9]", "");
                if (menu.isBlank() || digits.isBlank()) continue;
                try {
                    int price = Integer.parseInt(digits);
                    if (price > 0) entries.computeIfAbsent(name, ignored -> new ArrayList<>())
                            .add(new Entry(district, menu, price));
                } catch (NumberFormatException ignored) { }
            }
        }
        return entries;
    }

    @Override
    public Optional<Price> find(String restaurantName, String restaurantAddress, String officeAddress, String winningMenu) {
        String district = district(restaurantAddress);
        if (district.isBlank()) district = district(officeAddress);
        if (district.isBlank()) return Optional.empty();
        String finalDistrict = district;
        String wanted = normalize(winningMenu.replaceFirst("^(매장|배달)\\s*·\\s*", ""));
        return byName.getOrDefault(normalize(restaurantName), List.of()).stream()
                .filter(entry -> entry.district.equals(finalDistrict) && menuMatches(wanted, normalize(entry.menu)))
                .min((a, b) -> Integer.compare(a.won, b.won))
                .map(entry -> new Price(entry.menu, entry.won, SOURCE, AS_OF));
    }

    private static boolean menuMatches(String wanted, String listed) {
        if (wanted.isBlank()) return false;
        if (listed.contains(wanted) || wanted.contains(listed) && listed.length() >= 3) return true;
        if (wanted.equals("생선구이"))
            return List.of("고등어구이", "삼치구이", "갈치구이", "임연수구이").stream().anyMatch(listed::contains);
        if (wanted.equals("마파두부")) return listed.contains("마파두부");
        return false;
    }

    private static String district(String address) {
        Matcher matcher = DISTRICT.matcher(address == null ? "" : address);
        return matcher.find() ? matcher.group(1) : "";
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT).replaceAll("[\\s\\p{Punct}·]", "");
    }

    static List<String> parseCsv(String line) {
        List<String> cells = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cell.append('"'); i++;
                } else quoted = !quoted;
            } else if (c == ',' && !quoted) {
                cells.add(cell.toString()); cell.setLength(0);
            } else cell.append(c);
        }
        cells.add(cell.toString());
        return cells;
    }

    record Entry(String district, String menu, int won) {}
}
