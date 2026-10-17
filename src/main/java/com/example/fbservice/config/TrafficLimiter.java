package com.example.fbservice.config;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Per-instance safety limit; use a distributed limiter for multi-node production. */
@Component
public class TrafficLimiter {
    private record Window(long startMillis,int hits){}
    private final ConcurrentHashMap<String,Window> windows=new ConcurrentHashMap<>();

    public void check(String key,int maxHits,Duration period){
        long now=System.currentTimeMillis();
        long windowMs=period.toMillis();
        AtomicBoolean allowed=new AtomicBoolean(true);
        windows.compute(key,(k,current)->{
            if(current==null || now-current.startMillis()>=windowMs)
                return new Window(now,1);
            if(current.hits()>=maxHits){
                allowed.set(false);return current;
            }
            return new Window(current.startMillis(),current.hits()+1);
        });
        if(windows.size()>10000){
            windows.entrySet().removeIf(e->now-e.getValue().startMillis()>=windowMs);
        }
        if(!allowed.get())throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
            "Quá nhiều yêu cầu, vui lòng thử lại sau.");
    }
}
