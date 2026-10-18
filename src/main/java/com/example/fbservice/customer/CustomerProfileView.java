package com.example.fbservice.customer;

/** Account identity is read-only; email is shown only to the authenticated account owner. */
public record CustomerProfileView(String username, String email, String displayName, String phone) {}
