package com.example.fbservice.controller;

/** Limited public index DTO; no email, internal IDs or unrelated message text. */
public record ChatContactSummary(String contact, long unreadCount) {}
