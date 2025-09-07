package com.example.fbservice.service;

import com.example.fbservice.model.Order;
import com.example.fbservice.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderService {
  private final OrderRepository orderRepo;

  public OrderService(OrderRepository orderRepo) {
    this.orderRepo = orderRepo;
  }

  public Order create(Order o) { return orderRepo.save(o); }
  public List<Order> list() { return orderRepo.findAll(); }
}
