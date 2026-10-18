package com.example.fbservice.content;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Stateless preview over metrics explicitly provided by the user.
 * Never represents these numbers as verified Meta/TikTok/YouTube data.
 */
@Service
public class ContentMetricsService {
    private static final String SOURCE="USER_SUPPLIED_UNVERIFIED";
    private static final String CAVEAT =
        "Các số liệu do người dùng nhập, chưa được nền tảng xác minh. "
        +"Khung giờ UTC chỉ là tín hiệu tham khảo, không bảo đảm hiệu quả.";
    public record Summary(String source, int postCount, long totalViews,
                          long totalInteractions, BigDecimal interactionsPer100Views,
                          Integer candidateHourUtc, Integer supportingPosts,
                          String caveat) {}
    private static final class HourStats {
        int count;
        long views;
        long interactions;
        void add(long newViews,long newInteractions) {
            count++;
            views+=newViews;
            interactions+=newInteractions;
        }
    }

    public Summary analyze(ContentMetricsRequest request) {
        if(request==null||request.posts()==null
                ||request.posts().isEmpty()||request.posts().size()>100)
            throw invalid();
        LocalDateTime now=LocalDateTime.now(ZoneOffset.UTC);
        Map<Integer,HourStats> perHour=new HashMap<>();
        long views=0,interactions=0;
        var firstPlatform=request.posts().get(0)==null
            ? null : request.posts().get(0).platform();
        boolean onePlatform=true;
        for(ContentMetricsRequest.Metric metric:request.posts()) {
            if(metric==null||metric.platform()==null||metric.publishedAtUtc()==null
                ||metric.publishedAtUtc().isAfter(now)
                ||metric.publishedAtUtc().isBefore(now.minusYears(2))
                ||metric.views()<1||metric.views()>1_000_000_000L
                ||metric.likes()<0||metric.likes()>1_000_000_000L
                ||metric.comments()<0||metric.comments()>1_000_000_000L
                ||metric.shares()<0||metric.shares()>1_000_000_000L
                ||metric.saves()<0||metric.saves()>1_000_000_000L)
                throw invalid();
            long count=metric.likes()+metric.comments()+metric.shares()+metric.saves();
            views+=metric.views();
            interactions+=count;
            onePlatform &= metric.platform()==firstPlatform;
            perHour.computeIfAbsent(metric.publishedAtUtc().getHour(),h->new HourStats())
                .add(metric.views(),count);
        }

        // Do not fabricate a best time from sparse data, or compare unrelated platforms.
        Integer candidate=null;
        Integer support=null;
        if(onePlatform && request.posts().size()>=12) {
            var eligible=perHour.entrySet().stream()
                .filter(entry->entry.getValue().count>=4)
                .sorted((a,b)->{
                    int result=ratio(b.getValue().interactions,b.getValue().views)
                        .compareTo(ratio(a.getValue().interactions,a.getValue().views));
                    return result!=0?result:Integer.compare(a.getKey(),b.getKey());
                }).toList();
            if(eligible.size()>=3) {
                candidate=eligible.get(0).getKey();
                support=eligible.get(0).getValue().count;
            }
        }
        return new Summary(SOURCE,request.posts().size(),views,interactions,
            ratio(interactions,views),candidate,support,CAVEAT);
    }

    private static BigDecimal ratio(long interactions,long views) {
        return BigDecimal.valueOf(interactions).multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(views),2,RoundingMode.HALF_UP);
    }

    private static ResponseStatusException invalid() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "Cần 1–100 bài với số liệu không âm, lượt xem > 0 và giờ đăng UTC hợp lệ trong 2 năm gần đây.");
    }
}
