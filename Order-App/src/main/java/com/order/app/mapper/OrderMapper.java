package com.order.app.mapper;

import org.springframework.stereotype.Component;

import com.order.app.dto.CheckoutResponse;
import com.order.app.entity.CustomerOrder;

@Component
public class OrderMapper {

	public CheckoutResponse toResponse(CustomerOrder order) {
		return new CheckoutResponse(order.getId(), order.getStatus());
	}

}
