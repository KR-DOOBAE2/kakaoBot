package kr.jeommechurial.app;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
@Controller
public class PageController {
    @GetMapping("/r/{id}")
    public String round(@PathVariable("id") String id) { return "forward:/index.html"; }
}

