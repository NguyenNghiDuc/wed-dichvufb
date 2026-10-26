package com.example.fbservice.controller;

import com.example.fbservice.web.SearchText;
import java.text.Normalizer;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class HelpController {
 public record Question(String id,String category,String title,String answer){}
 private static final List<Question> QUESTIONS=List.of(
  new Question("create","orders","Đặt chiến dịch như thế nào?","Đăng ký, đăng nhập, chọn gói tư vấn và nhập đường dẫn đúng nền tảng. Giá được xác thực tại máy chủ trước khi nhận đơn."),
  new Question("manual","orders","Đơn có chạy tự động không?","Chưa kết nối nhà cung cấp tự động. Đơn được nhân viên xem xét và xử lý thủ công; theo dõi trạng thái tại Đơn của tôi hoặc Chiến dịch tư vấn."),
  new Question("cancel","orders","Hủy đơn được không?","Bạn có thể hủy đơn khi còn chờ duyệt. Mở chi tiết đơn và chọn Hủy yêu cầu. Đơn đã được xử lý không thể hủy theo cách này."),
  new Question("rating","orders","Đánh giá dịch vụ ở đâu?","Sau khi đơn hoàn thành, mở chi tiết và chọn Đánh giá / xem đánh giá dịch vụ. Mỗi đơn gửi một đánh giá 1–5 sao; nhận xét chỉ hiển thị với bạn và Admin."),
  new Question("lookup","orders","Tra cứu và lọc đơn hàng như thế nào?","Vào Đơn của tôi, nhập mã đơn hoặc chọn trạng thái rồi tìm kiếm. Đơn tư vấn nằm tại Chiến dịch tư vấn; bạn chỉ xem được đơn của mình."),
  new Question("growth","orders","GrowLab hỗ trợ phát triển tương tác như thế nào?","GrowLab tập trung nội dung, tối ưu hồ sơ và quảng cáo hợp lệ. Các kết nối nền tảng sử dụng quyền chính thức, không yêu cầu mật khẩu mạng xã hội."),
  new Question("payments","payments","Chức năng nạp tiền và thanh toán đã hoạt động chưa?","Chưa có cổng thanh toán thật được kết nối. Yêu cầu nạp tiền hiện là quy trình thủ công; không chuyển tiền dựa trên form thử nghiệm. Báo giá không phải xác nhận đã thanh toán."),
  new Question("password","account","Đổi hoặc quên mật khẩu thì làm gì?","Nếu còn đăng nhập, vào Hồ sơ và bảo mật để đổi mật khẩu bằng mật khẩu hiện tại. Quên mật khẩu bằng email chưa khả dụng; hãy liên hệ hỗ trợ. Không gửi mật khẩu hoặc OTP cho nhân viên."),
  new Question("sessions","account","Xem và thu hồi thiết bị đăng nhập ở đâu?","Vào Hồ sơ → Quản lý phiên và bảo mật. Bạn có thể thu hồi từng phiên bằng mật khẩu hiện tại hoặc đăng xuất tất cả thiết bị. Khi thu hồi phiên hiện tại bạn sẽ đăng xuất ngay."),
  new Question("locked","account","Vì sao đăng nhập bị khóa tạm thời?","Sau 5 lần sai mật khẩu trong 15 phút, cặp tài khoản và nguồn truy cập đó bị giới hạn 15 phút. Các nguồn khác không bị khóa theo. Chờ hết hạn rồi thử lại. Khóa tài khoản bởi Admin là trạng thái riêng cần liên hệ hỗ trợ."),
  new Question("warning","account","Cảnh báo trình duyệt hoặc mạng mới nghĩa là gì?","Website thông báo khi cặp IP và trình duyệt đăng nhập thay đổi. VPN hoặc mạng mới có thể gây cảnh báo. Nếu không phải bạn, hãy đổi mật khẩu và thu hồi các phiên cũ."),
  new Question("support","support","Liên hệ hỗ trợ ở đâu?","Đăng nhập rồi chọn Công cụ → Hỗ trợ → Tạo yêu cầu. Ghi rõ mã đơn và vấn đề; không gửi mật khẩu, mã OTP hoặc khóa API."),
  new Question("close","support","Đóng phiếu hỗ trợ khi đã giải quyết như thế nào?","Mở phiếu của bạn và chọn Đóng yêu cầu. Phiếu đã đóng không nhận thêm phản hồi; bạn có thể Mở lại yêu cầu kèm lý do để tiếp tục trao đổi và giữ lịch sử.")
 );
 @GetMapping("/help")
 public String help(@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="") String category,Model model){
  String query=SearchText.clean(q,120),selected=SearchText.clean(category,20);
  if(!List.of("","account","orders","payments","support").contains(selected))throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
  var words=fold(query).split("\\s+");
  var results=QUESTIONS.stream().filter(f->selected.isEmpty()||selected.equals(f.category()))
   .filter(f->Arrays.stream(words).allMatch(word->fold(f.title()+" "+f.answer()).contains(word))).toList();
  model.addAttribute("questions",results);model.addAttribute("filterQuery",query);model.addAttribute("category",selected);
  return "help";
 }
 private static String fold(String text){return Normalizer.normalize(text,Normalizer.Form.NFD).replaceAll("\\p{M}+","").toLowerCase(Locale.ROOT).replace('đ','d');}
}
