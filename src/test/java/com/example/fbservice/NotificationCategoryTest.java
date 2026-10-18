package com.example.fbservice;

import com.example.fbservice.notification.AccountNotification;
import com.example.fbservice.notification.NotificationCategory;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class NotificationCategoryTest {
    @Test
    void mapsExistingProducerTitlesWithoutInferringUnknownTypes() {
        assertThat(new AccountNotification("owner","Hỗ trợ #123 đã phản hồi","Có trả lời")
            .getCategory()).isEqualTo(NotificationCategory.SUPPORT);
        assertThat(new AccountNotification("owner","Đã nhận chiến dịch #123","Đơn đã ghi nhận")
            .getCategory()).isEqualTo(NotificationCategory.ORDER);
        assertThat(new AccountNotification("owner","Bảo mật tài khoản","Phiên thay đổi")
            .getCategory()).isEqualTo(NotificationCategory.ACCOUNT);
        assertThat(new AccountNotification("owner","Nội dung lạ","Chỉ là ghi chú")
            .getCategory()).isEqualTo(NotificationCategory.OTHER);
        assertThat(new AccountNotification("owner","<script>alert(1)</script>","text")
            .getCategoryLabel()).isEqualTo("Khác");
    }
}
