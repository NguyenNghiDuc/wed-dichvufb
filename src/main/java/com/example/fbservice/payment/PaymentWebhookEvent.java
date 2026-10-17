package com.example.fbservice.payment;

public record PaymentWebhookEvent(String eventId,Long depositRequestId,Long amountVnd,String status){}
