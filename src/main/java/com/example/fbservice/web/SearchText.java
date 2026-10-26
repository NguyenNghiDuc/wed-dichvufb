package com.example.fbservice.web;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
public final class SearchText {
 private SearchText(){}
 public static String clean(String value,int limit){
  String cleaned=value==null?"":value.strip();
  if(cleaned.length()>limit||cleaned.codePoints().anyMatch(Character::isISOControl))
   throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Bộ lọc tìm kiếm không hợp lệ.");
  return cleaned;
 }
 /** Treat SQL LIKE wildcard characters as literal user input. */
 public static String contains(String value){return "%"+value.toLowerCase(Locale.ROOT).replace("\\","\\\\").replace("%","\\%").replace("_","\\_")+"%";}
}
