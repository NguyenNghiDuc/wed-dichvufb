package com.example.fbservice.tiktok;
import com.example.fbservice.catalog.ServiceCatalogService;
import java.security.Principal;
import java.util.*;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
@Component
public class TiktokPageModel {
    private final TiktokOrderingService ordering;
    private final ServiceCatalogService catalog;
    public TiktokPageModel(TiktokOrderingService ordering,ServiceCatalogService catalog){this.ordering=ordering;this.catalog=catalog;}
    public void populate(Model model,Principal principal){
        var servers=ordering.available();
        model.addAttribute("servers",servers);
        model.addAttribute("countries",servers.stream().map(s->new TiktokOrderingService.Country(s.country(),s.countryName())).distinct().toList());
        model.addAttribute("totalTiktokOrders",ordering.totalOrders());
        model.addAttribute("recentOrders",principal==null?List.of():ordering.recent(principal.getName()));
        model.addAttribute("signedIn",principal!=null);
        model.addAttribute("services",catalog.available());
        Map<String,Object> defaults=new HashMap<>();
        defaults.put("checkout",false);defaults.put("quantity",100);defaults.put("note","");defaults.put("targetUrl","");defaults.put("serverVersion",0L);
        defaults.forEach((key,value)->{if(!model.containsAttribute(key))model.addAttribute(key,value);});
    }
}
