package com.example.fbservice.model;

import jakarta.persistence.*;

@Entity
public class BankAccount {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  private String bankName;
  private String accountNumber;
  private String accountHolder;

  // getters/setters
  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getBankName() { return bankName; }
  public void setBankName(String bankName) { this.bankName = bankName; }
  public String getAccountNumber() { return accountNumber; }
  public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
  public String getAccountHolder() { return accountHolder; }
  public void setAccountHolder(String accountHolder) { this.accountHolder = accountHolder; }
}
