package com.example.fbservice.service;

import com.example.fbservice.model.DepositRequest;
import com.example.fbservice.repository.DepositRequestRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DepositService {
  private final DepositRequestRepository repo;

  public DepositService(DepositRequestRepository repo) {
    this.repo = repo;
  }

  public DepositRequest save(DepositRequest dr) { return repo.save(dr); }
  public List<DepositRequest> findAll() { return repo.findAll(); }
}
