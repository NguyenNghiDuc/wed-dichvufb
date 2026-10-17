package com.example.fbservice.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class YouTubePublicChannelService {
    private static final String API_ENDPOINT = "https://www.googleapis.com/youtube/v3/channels";
    private final String apiKey;
    private final HttpClient http;
    private final ObjectMapper mapper;

    public YouTubePublicChannelService(@Value("${growth.youtube.api-key:}") String apiKey, ObjectMapper mapper) {
        this.apiKey=apiKey;
        this.mapper=mapper;
        this.http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    public Map<String,Object> channelSummary(String channelId) {
        if (channelId==null || !channelId.matches("UC[A-Za-z0-9_-]{22}"))
            throw new IllegalArgumentException("Channel ID phải bắt đầu UC và có 24 ký tự hợp lệ.");
        if(apiKey==null || apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"YouTube API chưa được cấu hình");
        }
        try {
            String url=API_ENDPOINT+"?part=snippet%2Cstatistics&id="+channelId+"&key="
                +java.net.URLEncoder.encode(apiKey,java.nio.charset.StandardCharsets.UTF_8);
            HttpRequest request=HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("Accept","application/json").GET().build();
            HttpResponse<String> response=http.send(request,HttpResponse.BodyHandlers.ofString());
            if(response.statusCode()!=200)throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"YouTube API returned a non-200 response");
            JsonNode items=mapper.readTree(response.body()).path("items");
            if(!items.isArray() || items.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy kênh");
            JsonNode item=items.get(0);
            JsonNode stats=item.path("statistics");
            Map<String,Object> result=new LinkedHashMap<>();
            result.put("provider","YOUTUBE");
            result.put("channelId",channelId);
            result.put("title",item.path("snippet").path("title").asText(""));
            result.put("subscribersHidden",stats.path("hiddenSubscriberCount").asBoolean(false));
            result.put("subscriberCount",stats.path("subscriberCount").asText(""));
            result.put("videoCount",stats.path("videoCount").asText(""));
            result.put("viewCount",stats.path("viewCount").asText(""));
            return result;
        } catch(ResponseStatusException ex) {
            throw ex;
        } catch(InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Provider request interrupted");
        } catch(Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Provider request unavailable");
        }
    }
}
