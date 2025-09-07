package com.example.fbservice.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class DepositRequest {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String bankName;
  private String accountNumber;
  private long amount;     // VND
  private String note;
  private String status = "PENDING"; // PENDING/APPROVED/REJECTED
  private LocalDateTime createdAt = LocalDateTime.now();

  // getters/setters
  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getBankName() { return bankName; }
  public void setBankName(String bankName) { this.bankName = bankName; }
  public String getAccountNumber() { return accountNumber; }
  public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
  public long getAmount() { return amount; }
  public void setAmount(long amount) { this.amount = amount; }
  public String getNote() { return note; }
  public void setNote(String note) { this.note = note; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
