package com.example.fbservice.review;
import com.example.fbservice.campaign.*;
import com.example.fbservice.tiktok.TiktokOrderRepository;
import java.time.*;
import java.util.Optional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
@Service
public class ServiceReviewService {
 private final ServiceReviewRepository reviews;private final CampaignOrderRepository campaigns;
 private final TiktokOrderRepository tiktok;private final Clock clock;
 public ServiceReviewService(ServiceReviewRepository reviews,CampaignOrderRepository campaigns,TiktokOrderRepository tiktok,Clock clock){this.reviews=reviews;this.campaigns=campaigns;this.tiktok=tiktok;this.clock=clock;}
 @Transactional(readOnly=true)
 public Optional<ServiceReview> mine(String owner,ReviewKind kind,Long id){
  authorize(owner,kind,id,false);return existing(kind,id);
 }
 @Transactional
 public ServiceReview create(String owner,ReviewKind kind,Long id,int rating,String comment){
  authorize(owner,kind,id,true);
  if(existing(kind,id).isPresent())throw new ResponseStatusException(HttpStatus.CONFLICT,"Đơn đã được đánh giá.");
  String text=comment==null?"":comment.strip();
  if(rating<1||rating>5||text.length()<10||text.length()>1000||text.codePoints().anyMatch(c->Character.isISOControl(c)&&c!='\n'&&c!='\r'&&c!='\t'))
   throw new IllegalArgumentException("Chọn 1–5 sao và nhập nhận xét từ 10 đến 1.000 ký tự.");
  return reviews.save(new ServiceReview(kind,id,owner,rating,text,LocalDateTime.now(clock)));
 }
 @Transactional(readOnly=true)
 public Page<ServiceReview> admin(int page,Integer rating,ReviewKind kind){
  if(page<0||page>10000||rating!=null&&(rating<1||rating>5))throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
  Specification<ServiceReview> filter=(root,query,b)->b.and(
   rating==null?b.conjunction():b.equal(root.get("rating"),rating),
   kind==null?b.conjunction():b.isNotNull(root.get(kind==ReviewKind.CAMPAIGN?"campaignOrderId":"tiktokOrderId")));
  return reviews.findAll(filter,PageRequest.of(page,20,Sort.by(Sort.Order.desc("createdAt"),Sort.Order.desc("id"))));
 }
 private Optional<ServiceReview> existing(ReviewKind kind,Long id){return kind==ReviewKind.CAMPAIGN?reviews.findByCampaignOrderId(id):reviews.findByTiktokOrderId(id);}
 private void authorize(String owner,ReviewKind kind,Long id,boolean lock){
  if(owner==null||owner.isBlank())throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
  if(kind==null||id==null||id<1)throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
  CampaignStatus status;
  if(kind==ReviewKind.CAMPAIGN){
   var order=(lock?campaigns.lockByIdAndOwner(id,owner):campaigns.findByIdAndOwnerUsername(id,owner))
    .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));status=order.getStatus();
  }else{
   var order=(lock?tiktok.lockById(id).filter(o->owner.equals(o.getOwnerUsername())):tiktok.findByIdAndOwnerUsername(id,owner))
    .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));status=order.getStatus();
  }
  if(status!=CampaignStatus.COMPLETED)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Chỉ đánh giá đơn đã hoàn thành.");
 }
}
