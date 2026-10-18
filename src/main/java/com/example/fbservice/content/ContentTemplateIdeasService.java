package com.example.fbservice.content;

import com.example.fbservice.campaign.SocialPlatform;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Offline human-editable marketing outlines, never mislabeled as model-generated
 * or inferred from social media data.
 */
@Service
public class ContentTemplateIdeasService {
    public record IdeaTemplate(String mode,String platform,String task,String language,
                               List<String> suggestions,String notice) {}

    public IdeaTemplate ideas(SocialPlatform platform,String topic,
                              AiContentTask task,AiContentLanguage language) {
        if(platform==null||topic==null||task==null||language==null)
            throw new IllegalArgumentException("Thiếu nền tảng, chủ đề hoặc kiểu nội dung.");
        String subject=topic.strip();
        if(subject.length()<3||subject.length()>100
                ||subject.chars().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException("Chủ đề cần từ 3 đến 100 ký tự hợp lệ.");
        if(task==AiContentTask.TIKTOK_IDEAS&&platform!=SocialPlatform.TIKTOK ||
           task==AiContentTask.YOUTUBE_SHORTS&&platform!=SocialPlatform.YOUTUBE)
            throw new IllegalArgumentException("Ý tưởng phải đúng nền tảng.");
        String hashtag=subject.replaceAll("[^\\p{L}\\p{N}]","");
        if(hashtag.isBlank())hashtag=language==AiContentLanguage.EN?"Learning":"NoiDung";
        if(hashtag.length()>40)hashtag=hashtag.substring(0,40);
        List<String> texts;
        if(language==AiContentLanguage.EN) {
            texts=switch(task) {
                case HASHTAGS -> List.of("#"+hashtag, "#PracticalTips", "#LearnTogether",
                    "#CreativeIdeas", "#HelpfulContent");
                case TIKTOK_IDEAS -> List.of(
                    "Hook: One useful lesson about "+subject+" in 20 seconds. Show 3 steps; ask for feedback.",
                    "Demonstration: compare two honest ways to approach "+subject+" with examples.",
                    "Q&A: answer a common beginner question about "+subject+" without unsupported claims.");
                case YOUTUBE_SHORTS -> List.of(
                    "Hook (0–3s): A practical question about "+subject+".",
                    "Main (3–35s): Show three helpful steps with on-screen examples.",
                    "End (35–50s): Summarize the lesson and invite viewers to share their experience.");
                case HEADLINES -> List.of(
                    "A beginner's guide to "+subject,
                    "Three practical ideas about "+subject,
                    "Questions to ask before starting with "+subject,
                    "How to approach "+subject+" step by step",
                    "What to check when learning about "+subject);
                default -> throw new IllegalArgumentException("Chưa có mẫu an toàn cho loại nội dung này.");
            };
        } else {
            texts=switch(task) {
                case HASHTAGS -> List.of("#"+hashtag, "#MeoHay", "#HocHoi",
                    "#NoiDungHuuIch", "#YTuongSangTao");
                case TIKTOK_IDEAS -> List.of(
                    "Mở đầu: Một mẹo hữu ích về "+subject+". Minh họa 3 bước, mời người xem chia sẻ.",
                    "Video hướng dẫn: So sánh hai cách tiếp cận "+subject+" bằng ví dụ thực tế.",
                    "Hỏi đáp: Giải đáp câu hỏi thường gặp về "+subject+" mà không khẳng định thiếu căn cứ.");
                case YOUTUBE_SHORTS -> List.of(
                    "Mở đầu (0–3 giây): Câu hỏi thực tế về "+subject+".",
                    "Nội dung (3–35 giây): Minh họa 3 bước bằng ví dụ rõ ràng.",
                    "Kết (35–50 giây): Tóm tắt và mời khán giả chia sẻ trải nghiệm.");
                case HEADLINES -> List.of(
                    "Hướng dẫn cơ bản về "+subject,
                    "3 ý tưởng hữu ích về "+subject,
                    "Những câu hỏi cần đặt ra khi tìm hiểu "+subject,
                    "Cách tiếp cận "+subject+" từng bước",
                    "Điều nên kiểm tra khi học về "+subject);
                default -> throw new IllegalArgumentException("Chưa có mẫu an toàn cho loại nội dung này.");
            };
        }
        return new IdeaTemplate("TEMPLATE_ONLY",platform.name(),task.name(),
            language.name(),List.copyOf(texts),
            language==AiContentLanguage.EN
                ? "Rule-based templates, not generative AI or verified platform data; review before using. Nothing is posted."
                : "Mẫu viết theo quy tắc, KHÔNG phải AI hoặc dữ liệu mạng xã hội xác minh; cần kiểm tra trước khi dùng. Không tự đăng.");
    }
}
