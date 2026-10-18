package com.example.fbservice.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Scheduling is shared with other jobs; no additional executor is created. */
@Configuration
@EnableScheduling
public class SecurityActivitySchedulingConfig {}
