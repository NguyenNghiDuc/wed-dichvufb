package com.example.fbservice.content;

import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/ai")
public class AiContentController {
    private final AiContentService service;
    public AiContentController(AiContentService service){this.service=service;}
    @PostMapping("/caption")
    public Map<String,String> generate(@Valid @RequestBody AiContentRequest input){
        return service.generate(input);
    }
}
