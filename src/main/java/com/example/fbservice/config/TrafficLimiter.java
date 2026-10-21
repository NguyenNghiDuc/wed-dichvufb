package com.example.fbservice.config;

import com.example.fbservice.security.ClientMetadata;
import java.time.Clock;
import java.time.Duration;
import javax.sql.DataSource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

/** Atomic shared counters survive process restarts and application transaction rollbacks. */
@Component
public class TrafficLimiter {
 private static final int MAX_ENTRIES=20000;
 private final JdbcTemplate jdbc;
 private final TransactionTemplate independent;
 private final Clock clock;
 public TrafficLimiter(DataSource source,PlatformTransactionManager transactions,Clock clock){
  this.jdbc=new JdbcTemplate(source);this.clock=clock;
  independent=new TransactionTemplate(transactions);
  independent.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
 }
 public void check(String key,int maxHits,Duration period){
  if(key==null||key.isBlank()||maxHits<1||period==null||period.isNegative()||period.isZero()||period.toMillis()<1)
   throw new IllegalArgumentException("Invalid limiter configuration");
  String hash=ClientMetadata.hash(key);long now=clock.millis(),expiry=Math.addExact(now,period.toMillis());
  if(consume(hash,maxHits,now,expiry))return;
  if(Boolean.TRUE.equals(independent.execute(s->jdbc.queryForObject("select count(*) from rate_limit_windows where bucket_hash=?",Integer.class,hash)>0)))throw limited();
  boolean capacity=Boolean.TRUE.equals(independent.execute(s->{
   jdbc.update("delete from rate_limit_windows where expires_at_ms<=?",now);
   return jdbc.queryForObject("select count(*) from rate_limit_windows",Integer.class)<MAX_ENTRIES;
  }));
  if(!capacity)throw limited();
  try{
   independent.executeWithoutResult(s->jdbc.update("insert into rate_limit_windows(bucket_hash,expires_at_ms,hits) values(?,?,1)",hash,expiry));
  }catch(DuplicateKeyException raced){
   if(!consume(hash,maxHits,now,expiry))throw limited();
  }
 }
 private boolean consume(String hash,int limit,long now,long expiry){
  return Boolean.TRUE.equals(independent.execute(s->jdbc.update(
   "update rate_limit_windows set hits=case when expires_at_ms<=? then 1 else hits+1 end, expires_at_ms=case when expires_at_ms<=? then ? else expires_at_ms end where bucket_hash=? and (expires_at_ms<=? or hits<?)",
   now,now,expiry,hash,now,limit)==1));
 }
 public void clear(String key){independent.executeWithoutResult(s->jdbc.update("delete from rate_limit_windows where bucket_hash=?",ClientMetadata.hash(key)));}
 private ResponseStatusException limited(){return new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Rate limit exceeded");}
}
