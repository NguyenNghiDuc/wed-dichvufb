package com.example.fbservice.payment;
/** Canonical result from a trusted bank/PSP adapter that independently verified the transaction. */
public record BankSettlementEvent(String eventId,String transactionId,Long amountVnd,
    String destinationAccount,String description,String status){}
