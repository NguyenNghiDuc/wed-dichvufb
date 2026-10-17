package com.example.fbservice.catalog;

import com.example.fbservice.campaign.CampaignServiceType;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServiceCatalogService {
    private final ServiceOfferingRepository items;
    private final ServiceCatalogAuditRepository events;

    public ServiceCatalogService(ServiceOfferingRepository items,ServiceCatalogAuditRepository events){
        this.items=items;this.events=events;
    }

    @Transactional(readOnly=true)
    public List<ServiceOffering> all() {
        return Arrays.stream(CampaignServiceType.values())
            .map(t -> items.findById(t.name()).orElseGet(() -> new ServiceOffering(t)))
            .toList();
    }

    @Transactional(readOnly=true)
    public List<ServiceOffering> available() {
        return all().stream().filter(ServiceOffering::isEnabled).toList();
    }

    @Transactional(readOnly=true)
    public long quote(CampaignServiceType type) {
        if(type==null)throw new IllegalArgumentException("Dịch vụ không hợp lệ.");
        ServiceOffering item=items.findById(type.name()).orElseGet(()->new ServiceOffering(type));
        if(!item.isEnabled())throw new IllegalArgumentException("Dịch vụ tạm dừng nhận đơn.");
        return item.getPriceVnd();
    }

    @Transactional
    public void update(String actor,CampaignServiceType type,long price,boolean enabled){
        update(actor, type, price, enabled, null, null, null);
    }

    /**
     * A browser snapshot (version + original price/enabled state) prevents stale
     * submissions even when the catalog row has not yet been persisted in H2.
     * @Version additionally rejects truly concurrent database writes.
     */
    @Transactional
    public void update(String actor,CampaignServiceType type,long price,boolean enabled,
                       Long expectedVersion,Long expectedPrice,Boolean expectedEnabled){
        if(type==null||actor==null||actor.isBlank())
            throw new IllegalArgumentException("Thông tin thay đổi không hợp lệ.");
        if(price<10000||price>50000000)
            throw new IllegalArgumentException("Giá dịch vụ cần từ 10.000 đến 50.000.000 VND.");
        ServiceOffering item=items.findById(type.name())
            .orElseGet(()->new ServiceOffering(type));
        if ((expectedVersion != null &&
                (expectedVersion < 0 || expectedVersion.longValue() != item.getVersion()))
            || (expectedPrice != null && expectedPrice.longValue() != item.getPriceVnd())
            || (expectedEnabled != null && expectedEnabled.booleanValue() != item.isEnabled()))
            throw new IllegalArgumentException("Bảng giá đã thay đổi. Tải lại trang trước khi lưu.");
        if(item.getPriceVnd()==price&&item.isEnabled()==enabled)
            throw new IllegalArgumentException("Chưa có thay đổi nào.");
        events.save(new ServiceCatalogAudit(actor,type.name(),
            item.getPriceVnd(),price,item.isEnabled(),enabled));
        item.update(price,enabled);
        // Flush inside the transaction so a concurrent @Version failure is reported
        // before returning to the Admin controller.
        items.saveAndFlush(item);
    }

    @Transactional(readOnly=true)
    public List<ServiceCatalogAudit> recent(){return events.findTop100ByOrderByCreatedAtDesc();}
}
