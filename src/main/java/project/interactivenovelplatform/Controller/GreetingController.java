package project.interactivenovelplatform.Controller;

import ch.qos.logback.core.model.Model;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller()
public class GreetingController {
    @GetMapping()
    public String greeting(
            @RequestParam(name="name",required = false,defaultValue = "name") String name,
            Model model
    ){
        model.addText(name);
        return "greeting";
    }
}
