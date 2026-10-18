package com.example.fbservice.service;

/** Internal legacy quote totals, NOT settled payments or delivered services. */
public record LegacyOrderSummary(long orderCount, long amountVnd) {}
