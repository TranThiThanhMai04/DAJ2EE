package DAJ2EE.demo;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String index() {
        // Trả về file index.html trong thư mục templates
        return "index";
    }
}
