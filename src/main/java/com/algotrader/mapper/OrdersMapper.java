package com.algotrader.mapper;

import org.springframework.stereotype.Service;

import com.algotrader.dto.ClientOrder;
import com.binance.api.client.domain.account.Order;

@Service
public class OrdersMapper {

    public ClientOrder toRecord(Order order) {
        return new ClientOrder(order.getClientOrderId(),
                order.getSide().toString(),
                order.getPrice(),
                order.getSymbol(),
                order.isWorking());
    }
}
