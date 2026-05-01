package com.n11.bootcamp.payment_service.mapper;

import com.n11.bootcamp.payment_service.dto.response.PaymentResponse;
import com.n11.bootcamp.payment_service.entity.Payment;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    PaymentResponse toResponse(Payment payment);
}
