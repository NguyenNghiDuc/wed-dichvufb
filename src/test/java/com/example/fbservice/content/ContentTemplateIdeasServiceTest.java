package com.example.fbservice.content;

import com.example.fbservice.campaign.SocialPlatform;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class ContentTemplateIdeasServiceTest {
    private final ContentTemplateIdeasService templates=new ContentTemplateIdeasService();

    @Test
    void bilingualTemplatesAreExplicitlyNotAiOrPublishing() {
        for (AiContentLanguage language:AiContentLanguage.values()) {
            for (AiContentTask task:new AiContentTask[]{
                AiContentTask.HASHTAGS,AiContentTask.HEADLINES,
                AiContentTask.TIKTOK_IDEAS,AiContentTask.YOUTUBE_SHORTS}) {
                SocialPlatform platform=task==AiContentTask.YOUTUBE_SHORTS
                    ?SocialPlatform.YOUTUBE:SocialPlatform.TIKTOK;
                var output=templates.ideas(platform,"Useful study skills",task,language);
                assertThat(output.mode()).isEqualTo("TEMPLATE_ONLY");
                assertThat(output.language()).isEqualTo(language.name());
                assertThat(output.task()).isEqualTo(task.name());
                assertThat(output.suggestions()).hasSizeGreaterThanOrEqualTo(3);
                assertThat(output.notice()).containsIgnoringCase("AI");
            }
        }
    }

    @Test
    void rejectsUnsupportedTasksWrongPlatformsAndUnsafeTopics() {
        assertThatThrownBy(()->templates.ideas(SocialPlatform.FACEBOOK,
            "Helpful tips",AiContentTask.TIKTOK_IDEAS,AiContentLanguage.VI))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->templates.ideas(SocialPlatform.TIKTOK,
            "Helpful tips",AiContentTask.YOUTUBE_SHORTS,AiContentLanguage.EN))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->templates.ideas(SocialPlatform.TIKTOK,
            "Helpful tips",AiContentTask.OPTIMIZE,AiContentLanguage.EN))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->templates.ideas(SocialPlatform.TIKTOK,
            "Invalid\nTopic",AiContentTask.HASHTAGS,AiContentLanguage.EN))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->templates.ideas(SocialPlatform.TIKTOK,
            "x".repeat(101),AiContentTask.HASHTAGS,AiContentLanguage.VI))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void hashtagTemplateOnlyEmitsSafeTagCharacters() {
        var suggestion=templates.ideas(SocialPlatform.INSTAGRAM,
            "new@<script> #ideas",AiContentTask.HASHTAGS,AiContentLanguage.EN);
        assertThat(suggestion.suggestions().get(0)).matches("#[\\p{L}\\p{N}]+");
        assertThat(suggestion.suggestions().get(0)).doesNotContain("<",">","@");
    }
}
