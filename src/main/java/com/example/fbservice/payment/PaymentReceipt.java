package com.example.fbservice.payment;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="payment_receipts",uniqueConstraints={
    @UniqueConstraint(name="uk_payment_event",columnNames={"event_id"}),
    @UniqueConstraint(name="uk_payment_deposit",columnNames={"deposit_request_id"})
})
public class PaymentReceipt {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="event_id",nullable=false,length=100) private String eventId;
    @Column(name="deposit_request_id",nullable=false) private Long depositRequestId;
    @Column(nullable=false) private long amountVnd;
    @Column(nullable=false) private LocalDateTime createdAt=LocalDateTime.now();

    protected PaymentReceipt(){}
    public PaymentReceipt(String eventId,Long depositRequestId,long amountVnd){
        this.eventId=eventId;this.depositRequestId=depositRequestId;this.amountVnd=amountVnd;
    }
    public Long getId(){return id;}
    public String getEventId(){return eventId;}
    public Long getDepositRequestId(){return depositRequestId;}
    public long getAmountVnd(){return amountVnd;}
    public LocalDateTime getCreatedAt(){return createdAt;}
}
