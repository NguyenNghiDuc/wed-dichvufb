package com.example.fbservice.campaign;

/** Private aggregated review counts; never reveals author, staff or review content. */
public record CustomerReviewSummary(long total, double average,
                                    long oneStar, long twoStars, long threeStars,
                                    long fourStars, long fiveStars) {}
