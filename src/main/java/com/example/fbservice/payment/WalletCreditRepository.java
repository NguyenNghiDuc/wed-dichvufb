package com.example.fbservice.payment;
import java.util.List;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WalletCreditRepository extends JpaRepository<WalletCredit,Long> {
    Optional<WalletCredit> findByBankTransactionId(String bankTransactionId);
    List<WalletCredit> findByDepositRequestIdIn(Collection<Long> depositIds);
    boolean existsByDepositRequestId(Long depositRequestId);
    List<WalletCredit> findTop50ByOwnerUsernameOrderByCreatedAtDescIdDesc(String ownerUsername);
    @Query("select coalesce(sum(c.amountVnd),0) from WalletCredit c where c.ownerUsername=:owner")
    long balanceForUser(@Param("owner") String owner);
}
