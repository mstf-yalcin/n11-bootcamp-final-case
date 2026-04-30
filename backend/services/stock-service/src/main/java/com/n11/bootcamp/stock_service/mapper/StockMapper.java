package com.n11.bootcamp.stock_service.mapper;

import com.n11.bootcamp.stock_service.dto.response.ReservationResponse;
import com.n11.bootcamp.stock_service.dto.response.StockResponse;
import com.n11.bootcamp.stock_service.entity.Inventory;
import com.n11.bootcamp.stock_service.entity.StockReservation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StockMapper {

    @Mapping(target = "available", expression = "java(inventory.getAvailable())")
    StockResponse toResponse(Inventory inventory);

    @Mapping(target = "createdAt", source = "createdAt")
    ReservationResponse toReservationResponse(StockReservation reservation);
}
