package com.example.fbservice.security;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.HexFormat;
public final class ClientMetadata {
 private ClientMetadata(){}
 public static String clean(String value,int max){
  String s=value==null?"":value.replaceAll("\\p{Cc}", " ").strip();
  return s.length()>max?s.substring(0,max):s;
 }
 public static String ip(HttpServletRequest request){return clean(request.getRemoteAddr(),64);}
 public static String device(HttpServletRequest request){return clean(request.getHeader("User-Agent"),200);}
 public static String hash(String sessionId){
  if(sessionId==null||sessionId.isEmpty())throw new IllegalArgumentException("Missing session identity");
  try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(sessionId.getBytes(StandardCharsets.UTF_8)));}
  catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}
 }
}
