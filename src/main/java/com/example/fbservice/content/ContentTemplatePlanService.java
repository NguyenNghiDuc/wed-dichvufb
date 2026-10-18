package com.example.fbservice.content;

import com.example.fbservice.campaign.SocialPlatform;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

/**
 * Transparent deterministic fallback for editorial planning. This is NOT AI
 * output, is not based on social platform analytics, and never schedules posts.
 */
@Service
public class ContentTemplatePlanService {
    public record Day(int day,String format,String idea) {}
    public record TemplatePlan(String mode,String platform,String topic,
                               int days,List<Day> suggestions,String notice) {}

    public TemplatePlan plan(SocialPlatform platform,String topic,int days) {
        if(platform==null||topic==null)
            throw new IllegalArgumentException("Nền tảng và chủ đề không hợp lệ.");
        String clean=topic.strip();
        if(clean.length()<3||clean.length()>100
                ||clean.chars().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException("Chủ đề cần từ 3 đến 100 ký tự hợp lệ.");
        if(days<7||days>30)
            throw new IllegalArgumentException("Kế hoạch từ 7 đến 30 ngày.");
        List<Day> suggestions=new ArrayList<>(days);
        String network=platform.getDisplayName();
        String[] formats={"Bài hướng dẫn","Câu hỏi thảo luận","Danh sách mẹo",
            "Ví dụ thực hành","Câu chuyện học hỏi"};
        for(int day=1;day<=days;day++) {
            String format=formats[(day-1)%formats.length];
            String idea=switch((day-1)%formats.length) {
                case 0 -> "Giải thích một khía cạnh hữu ích của "+clean;
                case 1 -> "Hỏi người xem về trải nghiệm thực tế với "+clean;
                case 2 -> "Chia sẻ 3 mẹo thực tế liên quan đến "+clean;
                case 3 -> "Đưa ví dụ minh họa rõ nguồn về "+clean;
                default -> "Chia sẻ điều đã học từ "+clean+" (không bịa câu chuyện)";
            };
            suggestions.add(new Day(day,format,idea+" trên "+network+"."));
        }
        return new TemplatePlan("TEMPLATE_ONLY",platform.name(),clean,days,
            List.copyOf(suggestions),
            "Kế hoạch theo mẫu có sẵn, KHÔNG phải AI và KHÔNG dựa trên thống kê thật. "
              +"Hãy chỉnh sửa trước khi lưu; hệ thống KHÔNG tự lên lịch hoặc đăng bài.");
    }
}
