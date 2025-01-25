package com.algotrader.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.algotrader.dto.ClientBalance;
import com.algotrader.service.PanicStrategyService;
import com.algotrader.service.TradingStrategy;

@RestController
public class TradingController {

	@Autowired
    private TradingStrategy strategy;
	@Autowired
	private PanicStrategyService panicStrategyService;

    @RequestMapping(method = RequestMethod.GET, value = "/balance/asset/{currency}")
    public ResponseEntity<ClientBalance> getCurrentClientBalance(@PathVariable String currency) {
        return ResponseEntity.ok(strategy.getAssetBalanceFor(currency));
    }

    @RequestMapping(method = RequestMethod.POST, value = "/orders/cancel-all")
    public ResponseEntity<String> cancelCurrentOrders() {
        strategy.cancelCurrentOrders();
        return ResponseEntity.ok("ALL_CANCELED");
    }

    @RequestMapping(method = RequestMethod.POST, value = "/orders/panic-sell")
    public ResponseEntity<String> panicSell() {
        strategy.panicSell();
        return ResponseEntity.ok("PANIC_SELL");
    }

    @RequestMapping(method = RequestMethod.POST, value = "/orders/sell-exit")
    public ResponseEntity<String> panicSellAndExit() {
        strategy.panicSell();
        panicStrategyService.initiateAppShutDown();
        return ResponseEntity.ok("PANIC_SELL&&EXIT");
    }
}
