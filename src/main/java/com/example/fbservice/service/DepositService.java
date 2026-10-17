package com.example.fbservice.service;

import com.example.fbservice.model.DepositRequest;
import com.example.fbservice.repository.DepositRequestRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class DepositService {
    public static final int HISTORY_PAGE_SIZE = 20;
    public static final int MAX_HISTORY_PAGE = 10000;
    private final DepositRequestRepository repo;

    public DepositService(DepositRequestRepository repo) {
        this.repo = repo;
    }

    public DepositRequest save(DepositRequest input, String username) {
        if (username == null || username.isBlank() || input == null
                || input.getAmount() < 1000 || input.getAmount() > 1000000000L
                || (input.getNote() != null && input.getNote().length() > 255)
                || (input.getBankName() != null && input.getBankName().length() > 255)
                || (input.getAccountNumber() != null && input.getAccountNumber().length() > 255)) {
            throw new IllegalArgumentException("Số tiền hoặc nội dung yêu cầu không hợp lệ.");
        }
        DepositRequest request = new DepositRequest();
        request.setOwnerUsername(username);
        request.setBankName(input.getBankName());
        request.setAccountNumber(input.getAccountNumber());
        request.setAmount(input.getAmount());
        request.setNote(input.getNote());
        request.setStatus("PENDING");
        return repo.save(request);
    }

    public Page<DepositRequest> history(String username, int page) {
        if (username == null || username.isBlank() || page < 0 || page > MAX_HISTORY_PAGE) {
            throw new IllegalArgumentException("Trang lịch sử không hợp lệ.");
        }
        return repo.findByOwnerUsernameOrderByCreatedAtDesc(username,
                PageRequest.of(page, HISTORY_PAGE_SIZE));
    }

    // Backwards-compatible bounded convenience method for callers that expect a list.
    public List<DepositRequest> findAllForUser(String username) {
        return history(username, 0).getContent();
    }
}
