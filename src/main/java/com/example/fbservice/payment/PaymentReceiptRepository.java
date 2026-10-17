package com.example.fbservice.payment;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface PaymentReceiptRepository extends JpaRepository<PaymentReceipt,Long> {
    Optional<PaymentReceipt> findByEventId(String eventId);
}
