package com.algotrader.service;

import static com.algotrader.util.Constants.ORDER_CANCELLED_MSG;
import static com.binance.api.client.domain.account.NewOrder.limitBuy;
import static com.binance.api.client.domain.account.NewOrder.limitSell;
import static com.binance.api.client.domain.account.NewOrder.marketBuy;
import static com.binance.api.client.domain.account.NewOrder.marketSell;

import java.util.List;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.algotrader.dto.ClientBalance;
import com.algotrader.exception.ApiException;
import com.algotrader.exception.NoCredentialsException;
import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.OrderStatus;
import com.binance.api.client.domain.TimeInForce;
import com.binance.api.client.domain.account.AssetBalance;
import com.binance.api.client.domain.account.NewOrder;
import com.binance.api.client.domain.account.NewOrderResponse;
import com.binance.api.client.domain.account.Order;
import com.binance.api.client.domain.account.request.CancelOrderRequest;
import com.binance.api.client.domain.account.request.OrderRequest;

@Service
public class ClientOrderService {

	
	
    private static final Logger logger = LoggerFactory.getLogger(ClientOrderService.class);

	
    @Value("${api.key}")
    private String apiKey;
    @Value("${api.secret}")
    private String apiSecret;
    @Value("${current.trade.pair}")
    private String currentTradePair;
    @Value("${enter.price}")
    private double enterPriceParam;
    @Value("${exit.price}")
    private double exitPriceParam;
    @Value("${exit.strategy.percentage}")
    private double exitLevel;

    private BinanceApiRestClient restClient;

    @PostConstruct
    public void postConstruct() {

        logger.info(">>> *** GRID BOT 2023 *** \n Current settings : currentTradePair = " + currentTradePair
                + "; \nenterPriceParam + " + enterPriceParam
                + "; \nexitPriceParam = " + exitPriceParam
                + "; \nexit level percentage = " + exitLevel + "%;");

        initCheckPing();
    }

    private void initCheckPing() {

        if (apiKey.isEmpty() || apiSecret.isEmpty()) {
            throw new NoCredentialsException();
        }
        restClient = BinanceApiClientFactory
                .newInstance(apiKey, apiSecret)
                .newRestClient();

        restClient.ping();
        try {
            restClient.getAccount();
        } catch (com.binance.api.client.exception.BinanceApiException apiException) {
            logger.info("BinanceApiException message: " + apiException.getError().getMsg());
            logger.info("Cause: " + apiException.getCause());
            throw new ApiException(apiException.getError().getMsg(), apiException.getCause());
        } catch (Exception exception) {
            logger.info("Cause: " + exception.getCause());
            throw new RuntimeException("Exception when first api call");
        }
    }

    public String createMarketBuy(String quantity) {

        logger.info(">>> Creating MARKET_BUY order ");
        return createOrder(marketBuy(currentTradePair, quantity));
    }

    public String createMarketSell(String quantity) {

        logger.info(">>> Creating MARKET_SELL order ");
        return createOrder(marketSell(currentTradePair, quantity));
    }

    public String createLimitBuy(String quantity, String price) {

        logger.info(">>> Creating LIMIT_BUY order ");
        return createOrder(limitBuy(currentTradePair, TimeInForce.GTC, quantity, price));
    }

    public String createLimitSell(String quantity, String price) {

        logger.info(">>> Creating LIMIT_SELL order ");
        return createOrder(limitSell(currentTradePair, TimeInForce.GTC, quantity, price));
    }

    public ClientBalance getBalanceForCurrency( String currency) {

        AssetBalance balance = restClient.getAccount().getAssetBalance(currency);
        return new ClientBalance(balance.getAsset(), balance.getFree(), balance.getLocked());
    }


    public String getCurrentPrice(String currentTradePair) {

        return restClient.getPrice(currentTradePair).getPrice();
    }

    public List<Order> getOpenOrders(String currentTradePair) {

        OrderRequest orderRequest = new OrderRequest(currentTradePair);
        return restClient.getOpenOrders(orderRequest);
    }

    public String cancelOrder(String currentTradePair, String clientOrderId) {

        CancelOrderRequest orderRequest = new CancelOrderRequest(currentTradePair, clientOrderId);
        var orderResponse = restClient.cancelOrder(orderRequest);
        logger.info(ORDER_CANCELLED_MSG + orderResponse.getStatus());
        return orderResponse.getStatus();
    }

    private String createOrder(NewOrder order) {

        NewOrderResponse newOrderResponse = restClient.newOrder(order);

        logger.info(">>> New Order status : " + newOrderResponse.getStatus());
        if (newOrderResponse.getStatus() == OrderStatus.REJECTED) {
            logger.info(">>> newOrderResponse REJECTED ");
        }

        logger.info(">>> Order created with status : " + newOrderResponse.getStatus() +
                " with price : " + newOrderResponse.getPrice());

        return newOrderResponse.getClientOrderId();
    }
}
