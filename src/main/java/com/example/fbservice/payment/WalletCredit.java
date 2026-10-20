package com.example.fbservice.payment;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name="wallet_ledger", uniqueConstraints={
    @UniqueConstraint(name="uk_wallet_deposit", columnNames="deposit_request_id"),
    @UniqueConstraint(name="uk_wallet_bank_txn", columnNames="bank_transaction_id")
}, indexes=@Index(name="idx_wallet_owner_created",columnList="owner_username,created_at"))
public class WalletCredit {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="owner_username",nullable=false,length=100) private String ownerUsername;
    @Column(name="deposit_request_id",nullable=false) private Long depositRequestId;
    @Column(name="bank_transaction_id",nullable=false,length=100) private String bankTransactionId;
    @Column(name="amount_vnd",nullable=false) private long amountVnd;
    @Column(name="created_at",nullable=false) private LocalDateTime createdAt=LocalDateTime.now(ZoneOffset.UTC);
    protected WalletCredit(){}
    public WalletCredit(String owner,Long deposit,String bankTransactionId,long amount){
        this.ownerUsername=owner; this.depositRequestId=deposit;
        this.bankTransactionId=bankTransactionId; this.amountVnd=amount;
    }
    public Long getId(){return id;}
    public String getOwnerUsername(){return ownerUsername;}
    public Long getDepositRequestId(){return depositRequestId;}
    public String getBankTransactionId(){return bankTransactionId;}
    public long getAmountVnd(){return amountVnd;}
    public LocalDateTime getCreatedAt(){return createdAt;}
}
