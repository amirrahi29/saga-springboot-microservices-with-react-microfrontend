package com.order.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.order.app.entity.CustomerOrder;

public interface OrderRepository extends JpaRepository<CustomerOrder, Long> {
}
