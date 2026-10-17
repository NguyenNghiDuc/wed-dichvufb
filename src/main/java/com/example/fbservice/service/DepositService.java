package com.example.fbservice.service;

import com.example.fbservice.model.DepositRequest;
import com.example.fbservice.repository.DepositRequestRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class DepositService {
    private final DepositRequestRepository repo;
    public DepositService(DepositRequestRepository repo){this.repo=repo;}

    public DepositRequest save(DepositRequest input,String username) {
        if(username==null||username.isBlank()||input==null||
           input.getAmount()<1000||input.getAmount()>1000000000L||
           (input.getNote()!=null && input.getNote().length()>255)||
           (input.getBankName()!=null && input.getBankName().length()>255)||
           (input.getAccountNumber()!=null && input.getAccountNumber().length()>255))
            throw new IllegalArgumentException("Số tiền hoặc nội dung yêu cầu không hợp lệ.");
        DepositRequest request=new DepositRequest();
        request.setOwnerUsername(username);
        request.setBankName(input.getBankName());
        request.setAccountNumber(input.getAccountNumber());
        request.setAmount(input.getAmount());
        request.setNote(input.getNote());
        request.setStatus("PENDING");
        return repo.save(request);
    }

    public List<DepositRequest> findAllForUser(String username) {
        return repo.findByOwnerUsernameOrderByCreatedAtDesc(username);
    }
}
