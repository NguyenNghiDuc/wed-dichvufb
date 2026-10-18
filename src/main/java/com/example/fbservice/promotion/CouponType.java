package com.example.fbservice.promotion;
public enum CouponType { FIXED("Giảm số tiền VND"),PERCENT("Giảm theo phần trăm");private final String title;CouponType(String title){this.title=title;}public String getTitle(){return title;} }
