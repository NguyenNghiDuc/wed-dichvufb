package com.example.fbservice.tiktok;

import com.example.fbservice.campaign.CampaignStatus;
import com.example.fbservice.notification.*;
import jakarta.persistence.criteria.Predicate;
import java.net.URI;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TiktokOrderingService {
    private final TiktokServerRepository servers;
    private final TiktokOrderRepository orders;
    private final TiktokOrderEventRepository events;
    private final AccountNotificationRepository notifications;
    public record ServerView(Long id,String name,String country,String countryName,String description,
                             long unitPriceVnd,int minimumQuantity,int maximumQuantity,long version) {}
    public record Country(String code,String name) {}
    public record Request(String targetUrl,String country,Long serverId,int quantity,String note) {}
    public record Quote(ServerView server,long totalPriceVnd) {}
    public TiktokOrderingService(TiktokServerRepository servers,TiktokOrderRepository orders,
        TiktokOrderEventRepository events,AccountNotificationRepository notifications){
        this.servers=servers;this.orders=orders;this.events=events;this.notifications=notifications;
    }
    public static String countryName(String code){
        return new Locale("",code).getDisplayCountry(Locale.forLanguageTag("vi"));
    }
    private ServerView view(TiktokServer s){return new ServerView(s.getId(),s.getName(),s.getCountry(),
        countryName(s.getCountry()),s.getDescription(),s.getUnitPriceVnd(),s.getMinimumQuantity(),s.getMaximumQuantity(),s.getVersion());}
    @Transactional(readOnly=true)
    public List<ServerView> available(){return servers.findByEnabledTrueOrderByCountryAscNameAscIdAsc().stream().map(this::view).toList();}
    @Transactional(readOnly=true)
    public List<TiktokServer> allServers(){return servers.findAllByOrderByIdDesc();}
    @Transactional
    public TiktokServer saveServer(Long id,String name,String country,String description,long price,int minimum,int maximum,boolean enabled,long version){
        String cleanName=name==null?"":name.strip();
        String cleanDescription=description==null?"":description.strip();
        if(cleanName.isBlank()||cleanName.length()>100||cleanDescription.length()>500||
            country==null||!Arrays.asList(Locale.getISOCountries()).contains(country)||price<1||price>1000000||minimum<1||maximum<minimum||maximum>1000000)
            throw new IllegalArgumentException("Kiểm tra tên, mã quốc gia ISO, đơn giá (1–1.000.000đ) và giới hạn số lượng (1–1.000.000).");
        TiktokServer server=id==null?new TiktokServer():servers.lockForUpdate(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        if(id!=null&&server.getVersion()!=version)throw new IllegalArgumentException("Server đã được cập nhật. Tải lại trang trước khi lưu.");
        server.setName(cleanName);server.setCountry(country);server.setDescription(cleanDescription);
        server.setUnitPriceVnd(price);server.setMinimumQuantity(minimum);server.setMaximumQuantity(maximum);server.setEnabled(enabled);
        return servers.save(server);
    }
    private TiktokServer validate(Request request){
        if(request==null||request.serverId()==null||request.targetUrl()==null||request.targetUrl().length()>512||request.note()==null||request.note().length()>1000)
            throw new IllegalArgumentException("Vui lòng nhập đầy đủ thông tin đơn hàng.");
        try{
            URI uri=URI.create(request.targetUrl().strip());
            String host=uri.getHost();
            if(!"https".equalsIgnoreCase(uri.getScheme())||host==null||uri.getUserInfo()!=null||uri.getPort()!=-1||
                !(host.equalsIgnoreCase("tiktok.com")||host.toLowerCase(Locale.ROOT).endsWith(".tiktok.com"))||
                uri.getPath()==null||!uri.getPath().matches("/@[a-zA-Z0-9._]+/?"))
                throw new IllegalArgumentException();
        }catch(IllegalArgumentException e){throw new IllegalArgumentException("Nhập link hồ sơ TikTok HTTPS, ví dụ https://www.tiktok.com/@username.");}
        TiktokServer server=servers.lockForOrder(request.serverId()).filter(TiktokServer::isEnabled)
            .orElseThrow(()->new IllegalArgumentException("Server không còn nhận đơn. Vui lòng chọn server khác."));
        if(!server.getCountry().equals(request.country()))throw new IllegalArgumentException("Server không thuộc quốc gia đã chọn.");
        if(request.quantity()<server.getMinimumQuantity()||request.quantity()>server.getMaximumQuantity())
            throw new IllegalArgumentException("Số lượng cho server này: "+server.getMinimumQuantity()+"–"+server.getMaximumQuantity()+".");
        return server;
    }
    @Transactional
    public Quote quote(Request request){TiktokServer server=validate(request);return new Quote(view(server),Math.multiplyExact(server.getUnitPriceVnd(),request.quantity()));}
    @Transactional
    public TiktokOrder create(String username,Request request,long expectedVersion){
        if(username==null||username.isBlank())throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        TiktokServer server=validate(request);
        if(server.getVersion()!=expectedVersion)throw new IllegalArgumentException("Giá hoặc cấu hình server đã thay đổi. Vui lòng xem lại báo giá trước khi đặt.");
        TiktokOrder order=new TiktokOrder();order.setOwnerUsername(username);order.setServerId(server.getId());
        order.setServerName(server.getName());order.setCountry(server.getCountry());order.setTargetUrl(request.targetUrl().strip());
        order.setQuantity(request.quantity());order.setUnitPriceVnd(server.getUnitPriceVnd());
        order.setTotalPriceVnd(Math.multiplyExact(server.getUnitPriceVnd(),request.quantity()));order.setNote(request.note().strip());
        orders.saveAndFlush(order);
        events.save(new TiktokOrderEvent(order.getId(),username,order.getStatus(),"Đã nhận yêu cầu, chờ xét duyệt thủ công."));
        notifications.save(new AccountNotification(username,"Đã nhận đơn TikTok #"+order.getId(),"Đơn đang chờ duyệt. Chưa có giao dịch thanh toán hoặc gửi sang nhà cung cấp."));
        return order;
    }
    @Transactional(readOnly=true)
    public TiktokOrder mine(String username,Long id){return orders.findByIdAndOwnerUsername(id,username).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));}
    @Transactional(readOnly=true)
    public TiktokOrder adminOrder(Long id){return orders.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));}
    @Transactional(readOnly=true)
    public List<TiktokOrder> recent(String username){return orders.findTop5ByOwnerUsernameOrderByCreatedAtDescIdDesc(username);}
    @Transactional(readOnly=true)
    public long totalOrders(){return orders.count();}
    @Transactional(readOnly=true)
    public Page<TiktokOrder> page(String username,Long id,CampaignStatus status,int page){
        if(page<0||page>10000||(id!=null&&id<=0))throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        Specification<TiktokOrder> filter=(root,query,builder)->{
            List<Predicate> p=new ArrayList<>();
            if(username!=null)p.add(builder.equal(root.get("ownerUsername"),username));
            if(id!=null)p.add(builder.equal(root.get("id"),id));
            if(status!=null)p.add(builder.equal(root.get("status"),status));
            return builder.and(p.toArray(new Predicate[0]));
        };
        return orders.findAll(filter,PageRequest.of(page,20,Sort.by(Sort.Direction.DESC,"createdAt","id")));
    }
    @Transactional(readOnly=true)
    public Page<TiktokOrderEvent> history(Long id,int page){
        if(page<0||page>10000)throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        return events.findByOrderIdOrderByCreatedAtDescIdDesc(id,PageRequest.of(page,20));
    }
    @Transactional
    public void cancel(String username,Long id){
        TiktokOrder order=orders.lockById(id).filter(o->o.getOwnerUsername().equals(username))
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        if(order.getStatus()!=CampaignStatus.PENDING_REVIEW)throw new IllegalArgumentException("Chỉ hủy được đơn đang chờ duyệt.");
        transition(order,CampaignStatus.CANCELLED,username,"Khách hàng hủy yêu cầu.");
    }
    @Transactional
    public void updateStatus(String actor,Long id,CampaignStatus status){
        TiktokOrder order=orders.lockById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        boolean valid=switch(order.getStatus()){
            case PENDING_REVIEW->status==CampaignStatus.APPROVED||status==CampaignStatus.REJECTED;
            case APPROVED->status==CampaignStatus.IN_PROGRESS||status==CampaignStatus.REJECTED;
            case IN_PROGRESS->status==CampaignStatus.COMPLETED;
            default->false;
        };
        if(!valid)throw new IllegalArgumentException("Trạng thái tiếp theo không hợp lệ.");
        transition(order,status,actor,"Quản trị viên cập nhật trạng thái xử lý nội bộ.");
    }
    private void transition(TiktokOrder order,CampaignStatus status,String actor,String message){
        order.setStatus(status);events.save(new TiktokOrderEvent(order.getId(),actor,status,message));
        notifications.save(new AccountNotification(order.getOwnerUsername(),"Cập nhật đơn TikTok #"+order.getId(),status.getDisplayName()+". Đây là trạng thái xử lý nội bộ."));
    }
}
