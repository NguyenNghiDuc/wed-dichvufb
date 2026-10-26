package com.example.fbservice.broadcast;
import com.example.fbservice.repository.UserRepository;
import java.sql.Timestamp;
import java.time.*;
import java.util.List;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
@Service
public class BroadcastService {
 public static final int MAX_RECIPIENTS=5000;
 private final SystemBroadcastRepository broadcasts;private final BroadcastEventRepository events;
 private final UserRepository users;private final JdbcTemplate jdbc;private final Clock clock;
 public BroadcastService(SystemBroadcastRepository broadcasts,BroadcastEventRepository events,UserRepository users,JdbcTemplate jdbc,Clock clock){this.broadcasts=broadcasts;this.events=events;this.users=users;this.jdbc=jdbc;this.clock=clock;}
 @Transactional
 public SystemBroadcast create(String actor,String title,String message,BroadcastAudience audience){
  authorize(actor);String heading=clean(title,4,120),body=clean(message,10,500);
  if(audience==null)throw new IllegalArgumentException("Chọn nhóm người nhận.");
  var now=LocalDateTime.now(clock);var draft=broadcasts.saveAndFlush(new SystemBroadcast(heading,body,audience,actor,now));
  events.save(new BroadcastEvent(draft.getId(),actor,"CREATED",now));return draft;
 }
 @Transactional
 public SystemBroadcast send(String actor,Long id,int confirmedRecipients){
  authorize(actor);var draft=locked(id);
  if(draft.getStatus()==BroadcastStatus.SENT)return draft; // Retry after a lost response is safe.
  if(draft.getStatus()!=BroadcastStatus.DRAFT)throw new IllegalArgumentException("Bản nháp đã hủy không thể gửi.");
  long count=recipients(draft.getAudience());
  if(count<1||count>MAX_RECIPIENTS)throw new IllegalArgumentException("Nhóm nhận phải có 1–5.000 tài khoản đang hoạt động.");
  if(count!=confirmedRecipients)throw new IllegalArgumentException("Số người nhận đã thay đổi. Xem lại bản nháp và xác nhận số mới trước khi gửi.");
  var now=LocalDateTime.now(clock);
  int inserted=jdbc.update("insert into account_notifications(owner_username,title,message,created_at,broadcast_id) select username,?,?,?,? from users where enabled=true and (?='ALL' or role=?)",
    draft.getTitle(),draft.getMessage(),Timestamp.valueOf(now),draft.getId(),draft.getAudience().name(),draft.getAudience().name());
  // Membership may change between the count and insert. Roll back the entire fan-out if so.
  if(inserted!=confirmedRecipients)throw new IllegalArgumentException("Danh sách người nhận vừa thay đổi. Chưa có thông báo nào được gửi; hãy kiểm tra và gửi lại.");
  draft.sent(actor,inserted,now);events.save(new BroadcastEvent(id,actor,"SENT",now));return draft;
 }
 @Transactional
 public void cancel(String actor,Long id){
  authorize(actor);var draft=locked(id);
  if(draft.getStatus()==BroadcastStatus.CANCELLED)return;
  if(draft.getStatus()!=BroadcastStatus.DRAFT)throw new IllegalArgumentException("Thông báo đã gửi không thể thu hồi.");
  var now=LocalDateTime.now(clock);draft.cancel(actor,now);events.save(new BroadcastEvent(id,actor,"CANCELLED",now));
 }
 @Transactional(readOnly=true)
 public SystemBroadcast detail(Long id){return broadcasts.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));}
 @Transactional(readOnly=true)
 public long recipients(BroadcastAudience audience){
  return jdbc.queryForObject("select count(*) from users where enabled=true and (?='ALL' or role=?)",Long.class,audience.name(),audience.name());
 }
 @Transactional(readOnly=true)
 public Page<SystemBroadcast> list(int page,BroadcastStatus status){
  if(page<0||page>10000)throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
  var paging=PageRequest.of(page,20,Sort.by(Sort.Order.desc("createdAt"),Sort.Order.desc("id")));
  return status==null?broadcasts.findAll(paging):broadcasts.findByStatus(status,paging);
 }
 @Transactional(readOnly=true)
 public List<BroadcastEvent> history(Long id){detail(id);return events.findByBroadcastIdOrderByCreatedAtAscIdAsc(id);}
 private SystemBroadcast locked(Long id){return broadcasts.lockById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));}
 private void authorize(String actor){users.lockByUsername(actor).filter(u->u.isEnabled()&&"ADMIN".equals(u.getRole())).orElseThrow(()->new ResponseStatusException(HttpStatus.FORBIDDEN));}
 private String clean(String value,int min,int max){String s=value==null?"":value.strip();if(s.length()<min||s.length()>max||s.codePoints().anyMatch(c->Character.isISOControl(c)&&c!='\n'&&c!='\r'&&c!='\t'))throw new IllegalArgumentException("Tiêu đề cần 4–120 ký tự, nội dung 10–500 ký tự và không chứa ký tự điều khiển.");return s;}
}
