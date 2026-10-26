package com.example.fbservice.promotion;
import com.example.fbservice.repository.UserRepository;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.web.server.ResponseStatusException;
@Service
public class CouponService {
 private final CouponRepository coupons;private final CouponUseRepository uses;private final CouponAuditRepository audit;private final UserRepository users;private final Clock clock;
 public CouponService(CouponRepository coupons,CouponUseRepository uses,CouponAuditRepository audit,UserRepository users,Clock clock){this.coupons=coupons;this.uses=uses;this.audit=audit;this.users=users;this.clock=clock;}
 public record Definition(String code,CouponType type,long amount,long maximumDiscountVnd,long minimumOrderVnd,int maximumUses,int perUserLimit,@org.springframework.format.annotation.DateTimeFormat(iso=org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) LocalDateTime startsAt,@org.springframework.format.annotation.DateTimeFormat(iso=org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) LocalDateTime endsAt){}
 public record Quote(Long couponId,String couponCode,long originalPriceVnd,long discountVnd,long totalPriceVnd){}
 @Transactional
 public Coupon create(String actor,Definition d){
  authorize(actor);String code=normalize(d.code());
  if(code.isEmpty()||d.type()==null||d.amount()<1||d.amount()>(d.type()==CouponType.PERCENT?100:50000000)||d.maximumDiscountVnd()<0||d.maximumDiscountVnd()>50000000||d.minimumOrderVnd()<0||d.minimumOrderVnd()>50000000||d.maximumUses()<1||d.maximumUses()>100000||d.perUserLimit()<1||d.perUserLimit()>100||d.perUserLimit()>d.maximumUses()||d.startsAt()==null||d.endsAt()==null||!d.endsAt().isAfter(d.startsAt()))
   throw new IllegalArgumentException("Kiểm tra giá trị giảm, mức tối thiểu, lượt dùng và khoảng thời gian UTC.");
  if(coupons.findByCode(code).isPresent())throw new IllegalArgumentException("Mã này đã tồn tại.");
  var now=LocalDateTime.now(clock);var c=coupons.saveAndFlush(new Coupon(code,d.type(),d.amount(),d.maximumDiscountVnd(),d.minimumOrderVnd(),d.maximumUses(),d.perUserLimit(),d.startsAt(),d.endsAt(),now));
  audit.save(new CouponAudit(c.getId(),actor,"CREATED",now));return c;
 }
 @Transactional(readOnly=true)
 public Quote preview(String owner,String code,long original){return calculate(owner,code,original,false);}
 @Transactional(propagation=Propagation.MANDATORY)
 public Quote reserve(String owner,String code,long original){return calculate(owner,code,original,true);}
 @Transactional(propagation=Propagation.MANDATORY)
 public void recordUse(Quote quote,Long orderId,String owner){if(quote.couponId()!=null)uses.save(new CouponUse(quote.couponId(),orderId,owner,quote.discountVnd(),LocalDateTime.now(clock)));}
 private Quote calculate(String owner,String code,long original,boolean reserve){
  if(owner==null||owner.isBlank())throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
  if(original<0||original>50000000)throw new IllegalArgumentException("Giá gốc không hợp lệ.");
  String cleaned=normalize(code);if(cleaned.isEmpty())return new Quote(null,null,original,0,original);
  var c=(reserve?coupons.lockCode(cleaned):coupons.findByCode(cleaned)).orElseThrow(()->new IllegalArgumentException("Mã giảm giá không khả dụng."));
  var now=LocalDateTime.now(clock);
  if(!c.isEnabled()||now.isBefore(c.getStartsAt())||!now.isBefore(c.getEndsAt())||c.getUsedCount()>=c.getMaximumUses()||original<c.getMinimumOrderVnd()||uses.countByCouponIdAndOwnerUsername(c.getId(),owner)>=c.getPerUserLimit())throw new IllegalArgumentException("Mã chưa áp dụng được: kiểm tra hạn dùng, giá trị đơn và giới hạn lượt dùng.");
  long discount=c.getType()==CouponType.PERCENT?Math.multiplyExact(original,c.getAmount())/100:c.getAmount();
  if(c.getMaximumDiscountVnd()>0)discount=Math.min(discount,c.getMaximumDiscountVnd());discount=Math.min(original,discount);
  if(discount<1)throw new IllegalArgumentException("Mã không tạo giảm giá cho gói này.");
  if(reserve)c.consume();return new Quote(c.getId(),c.getCode(),original,discount,original-discount);
 }
 @Transactional
 public void setEnabled(String actor,Long id,boolean expected,boolean enabled){authorize(actor);var c=coupons.lockId(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));if(c.isEnabled()!=expected||expected==enabled)throw new IllegalArgumentException("Trạng thái mã đã thay đổi. Tải lại trang để kiểm tra.");c.setEnabled(enabled);audit.save(new CouponAudit(id,actor,enabled?"ENABLED":"DISABLED",LocalDateTime.now(clock)));}
 @Transactional(readOnly=true) public Coupon detail(Long id){return coupons.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));}
 @Transactional(readOnly=true) public Page<Coupon> list(int page,Boolean enabled){var p=paging(page);return enabled==null?coupons.findAll(p):coupons.findByEnabled(enabled,p);}
 @Transactional(readOnly=true) public Page<CouponUse> uses(Long id,int page){detail(id);return uses.findByCouponId(id,paging(page));}
 @Transactional(readOnly=true) public List<CouponAudit> audit(Long id){detail(id);return audit.findTop20ByCouponIdOrderByCreatedAtDescIdDesc(id);}
 private Pageable paging(int page){if(page<0||page>10000)throw new ResponseStatusException(HttpStatus.BAD_REQUEST);return PageRequest.of(page,20,Sort.by(Sort.Order.desc("createdAt"),Sort.Order.desc("id")));}
 private void authorize(String actor){users.lockByUsername(actor).filter(u->u.isEnabled()&&"ADMIN".equals(u.getRole())).orElseThrow(()->new ResponseStatusException(HttpStatus.FORBIDDEN));}
 private String normalize(String code){String s=code==null?"":code.strip().toUpperCase(Locale.ROOT);if(!s.isEmpty()&&!s.matches("[A-Z0-9_-]{4,32}"))throw new IllegalArgumentException("Mã cần 4–32 ký tự chữ, số, gạch ngang hoặc gạch dưới.");return s;}
}
