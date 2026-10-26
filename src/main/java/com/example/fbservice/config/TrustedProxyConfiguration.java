package com.example.fbservice.config;

import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Native Tomcat forwarding is supported only with an explicit operator-owned peer allowlist. */
@Component
public class TrustedProxyConfiguration {
 public TrustedProxyConfiguration(@Value("${server.forward-headers-strategy:none}") String strategy,
                                  @Value("${server.tomcat.remoteip.internal-proxies:}") String peers){
  if(!"none".equalsIgnoreCase(strategy)&&!"native".equalsIgnoreCase(strategy))
   throw new IllegalArgumentException("Use forwarding strategy none or native with an explicit trusted proxy regex");
  if("native".equalsIgnoreCase(strategy)){
   if(peers.isBlank()||peers.equals(".*")||peers.equals("^.*$"))
    throw new IllegalArgumentException("Native forwarding requires a restrictive TRUSTED_PROXY_REGEX");
   Pattern.compile(peers);
  }
 }
}
