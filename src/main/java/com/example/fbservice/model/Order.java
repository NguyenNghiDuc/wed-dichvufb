package com.example.fbservice.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
public class Order {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String linkOrder;
  private String serverName;
  private int quantity;
  private String note;
  private int pricePerUnit = 10;
  private int totalPrice;

  private LocalDateTime createdAt = LocalDateTime.now();

  @PrePersist @PreUpdate
  public void calcTotal() {
    this.totalPrice = quantity * pricePerUnit;
  }

  // getters/setters
  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getLinkOrder() { return linkOrder; }
  public void setLinkOrder(String linkOrder) { this.linkOrder = linkOrder; }
  public String getServerName() { return serverName; }
  public void setServerName(String serverName) { this.serverName = serverName; }
  public int getQuantity() { return quantity; }
  public void setQuantity(int quantity) { this.quantity = quantity; }
  public String getNote() { return note; }
  public void setNote(String note) { this.note = note; }
  public int getPricePerUnit() { return pricePerUnit; }
  public void setPricePerUnit(int pricePerUnit) { this.pricePerUnit = pricePerUnit; }
  public int getTotalPrice() { return totalPrice; }
  public void setTotalPrice(int totalPrice) { this.totalPrice = totalPrice; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
