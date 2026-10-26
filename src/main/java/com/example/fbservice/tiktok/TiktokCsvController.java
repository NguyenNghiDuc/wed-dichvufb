package com.example.fbservice.tiktok;
import com.example.fbservice.campaign.CampaignStatus;
import com.example.fbservice.campaign.CampaignCsvExportService;
import java.security.Principal;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
@RestController
public class TiktokCsvController {
 private final TiktokOrderingService ordering;
 public TiktokCsvController(TiktokOrderingService ordering){this.ordering=ordering;}
 @GetMapping(value="/tiktok/orders/export.csv",produces="text/csv;charset=UTF-8")
 @Transactional(readOnly=true)
 public ResponseEntity<String> mine(@RequestParam(required=false) Long orderId,
      @RequestParam(required=false) CampaignStatus status,Principal principal){return export(principal.getName(),orderId,status,"my-tiktok-orders.csv");}
 @GetMapping(value="/admin/tiktok/orders/export.csv",produces="text/csv;charset=UTF-8")
 @Transactional(readOnly=true)
 public ResponseEntity<String> admin(@RequestParam(required=false) Long orderId,
      @RequestParam(required=false) CampaignStatus status){return export(null,orderId,status,"tiktok-orders.csv");}
 private ResponseEntity<String> export(String username,Long orderId,CampaignStatus status,String filename){
  StringBuilder csv=new StringBuilder("\uFEFFid,owner,server,country,quantity,unit_price_vnd,total_vnd,status,created_at,target_url,note\r\n");
  int count=0;
  for(int pageNumber=0;count<5000;pageNumber++){
   var page=ordering.page(username,orderId,status,pageNumber);
   for(var order:page.getContent()){
    if(count>=5000)break;
    Object[] cells={order.getId(),order.getOwnerUsername(),order.getServerName(),order.getCountry(),order.getQuantity(),
     order.getUnitPriceVnd(),order.getTotalPriceVnd(),order.getStatus(),order.getCreatedAt(),order.getTargetUrl(),order.getNote()};
    for(int i=0;i<cells.length;i++){if(i>0)csv.append(',');csv.append(CampaignCsvExportService.escape(cells[i]));}
    csv.append("\r\n");count++;
   }
   if(!page.hasNext())break;
  }
  return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\""+filename+"\"")
   .header(HttpHeaders.CACHE_CONTROL,"no-store").contentType(MediaType.parseMediaType("text/csv;charset=UTF-8")).body(csv.toString());
 }
}
