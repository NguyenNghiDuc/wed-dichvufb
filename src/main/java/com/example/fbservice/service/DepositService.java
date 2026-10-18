package com.example.fbservice.service;

import com.example.fbservice.model.DepositRequest;
import com.example.fbservice.repository.DepositRequestRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
    public Page<DepositRequest> page(String owner,String status,int page) {
        if(page<0||page>10000||status!=null&&!status.isBlank()&&!Set.of("PENDING","APPROVED","CANCELLED").contains(status))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        Specification<DepositRequest> filter=(root,query,b)->b.and(
            owner==null?b.conjunction():b.equal(root.get("ownerUsername"),owner),
            status==null||status.isBlank()?b.conjunction():b.equal(root.get("status"),status));
        return repo.findAll(filter,PageRequest.of(page,20,
            Sort.by(Sort.Order.desc("createdAt"),Sort.Order.desc("id"))));
    }

    public DepositRequest detail(Long id,String owner) {
        return repo.findByIdAndOwnerUsername(id,owner).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @Transactional
    public void cancel(Long id,String owner) {
        var request=repo.findLockedById(id).filter(d->owner.equals(d.getOwnerUsername()))
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        if(!"PENDING".equals(request.getStatus()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Yêu cầu không còn chờ xử lý.");
        request.setStatus("CANCELLED");
    }

}
