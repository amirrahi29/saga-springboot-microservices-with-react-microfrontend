package com.payment.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.payment.app.entity.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
}
