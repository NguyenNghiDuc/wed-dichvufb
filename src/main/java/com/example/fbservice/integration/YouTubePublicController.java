package com.example.fbservice.integration;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/integrations/youtube")
public class YouTubePublicController {
    private final YouTubePublicChannelService service;
    public YouTubePublicController(YouTubePublicChannelService service){this.service=service;}
    @GetMapping("/channels/{id}")
    public Map<String,Object> getPublicChannel(@PathVariable String id){
        try {return service.channelSummary(id);}
        catch(IllegalArgumentException e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,e.getMessage());}
    }
}
