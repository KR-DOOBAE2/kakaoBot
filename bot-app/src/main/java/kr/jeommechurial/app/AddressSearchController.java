package kr.jeommechurial.app;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/addresses")
public class AddressSearchController {
    private final AddressLookup lookup;

    public AddressSearchController(AddressLookup lookup) {
        this.lookup = lookup;
    }

    @GetMapping("/search")
    public List<AddressLookup.AddressResult> search(@RequestParam String q) {
        String query = q.strip();
        if (query.length() < 2 || query.length() > 100)
            throw new IllegalArgumentException("주소 검색어는 2~100자로 입력해주세요.");
        return lookup.searchAddresses(query);
    }
}
