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

    public DepositRequest save(DepositRequest input, String username) {
        if (input.getAmount() < 1000 || input.getAmount() > 1000000000L) {
            throw new IllegalArgumentException("Số tiền cần từ 1.000 đến 1.000.000.000 VND.");
        }
        DepositRequest request = new DepositRequest();
        request.setOwnerUsername(username);
        request.setBankName(input.getBankName());
        request.setAccountNumber(input.getAccountNumber());
        request.setAmount(input.getAmount());
        request.setNote(input.getNote());
        request.setStatus("PENDING"); // No balance credit until a verified payment integration.
        return repo.save(request);
    }

    public List<DepositRequest> findAllForUser(String username) {
        return repo.findByOwnerUsernameOrderByCreatedAtDesc(username);
    }
}
