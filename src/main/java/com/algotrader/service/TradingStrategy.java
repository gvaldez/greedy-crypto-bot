package com.algotrader.service;

import static com.algotrader.util.Constants.BTC;
import static com.algotrader.util.Constants.BUY_ORDER_MSG;
import static com.algotrader.util.Constants.BUY_ORDER_SIDE;
import static com.algotrader.util.Constants.ORDER_UNAVAILABLE_MSG;
import static com.algotrader.util.Constants.SELL_ORDER_MSG;
import static com.algotrader.util.Constants.SELL_ORDER_SIDE;
import static com.algotrader.util.Constants.UNEXPECTED_MSG;
import static com.algotrader.util.Constants.USDT;
import static com.algotrader.util.Constants.WAITING_MSG;
import static java.lang.Double.parseDouble;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.algotrader.cache.BackgroundCache;
import com.algotrader.dto.ClientBalance;
import com.algotrader.dto.ClientOrder;
import com.algotrader.exception.CacheException;
import com.algotrader.mapper.OrdersMapper;
import com.algotrader.util.Converter;

@Service
public class TradingStrategy {

	
    private static final Logger logger = LoggerFactory.getLogger(TradingStrategy.class);

	
    private boolean isFirstRun = Boolean.TRUE;

    @Value("${current.trade.pair}")
    private String currentTradePair;
    @Value("${current.coin}")
    private String currentCoin;
    @Value("${exit.strategy.percentage}")
    private double exitLevel;
    @Value("${enter.price}")
    private double enterPriceParam;
    @Value("${exit.price}")
    private double exitPriceParam;
    @Value("${buy.quantity}")
    private String buyQuantity;
    @Value("${enough.asset.balance}")
    private double enoughAssetBalance;

    @Autowired
    private BackgroundCache trendCache;
    
    @Autowired
    private PanicStrategyService panicStrategyService;
    
    @Autowired
    private ClientOrderService orderService;
    
    @Autowired
    private OrdersMapper ordersMapper;

    public ClientBalance getAssetBalanceFor(final String currency) 
    {

        return orderService.getBalanceForCurrency(currency);
    }

    public void saveCurrentPrice() 
    {

        String currentPrice = orderService.getCurrentPrice(currentTradePair);
        trendCache.save(parseDouble(currentPrice));
    }

    public void execute() 
    {

        if (!canEnter()) 
        {
            logger.info(WAITING_MSG);
            return;
        }
        List<ClientOrder> openOrders = getOpenOrders();
        ClientOrder currentOrder = null;
        if (!openOrders.isEmpty()) 
        {

            currentOrder = openOrders.stream()
                    .filter(Objects::nonNull)
                    .filter(ClientOrder::isWorking)
                    .filter(order -> Objects.equals(currentTradePair, order.symbol()))
                    .findAny().orElseThrow(
                            () -> new RuntimeException(UNEXPECTED_MSG));
        }

        if (isFirstRun) 
        {
            logger.info((currentOrder != null) ? currentOrder.toString() : ORDER_UNAVAILABLE_MSG);
            isFirstRun = Boolean.FALSE;
            logger.info(">>> Asset Balance for BTC = " + getAssetBalanceForCurrency(BTC));
            logger.info(">>> Asset Balance for USDT = " + getAssetBalanceForCurrency(USDT));
        }
        if (panicStrategyService.isThreshold()) 
        {
            logger.info(">>> Provide emergency panic sell");
            String orderId = panicSell();
            logger.info(">>> EMERGENCY orderId is " + orderId);
            panicStrategyService.initiateAppShutDown();
        }
        if (openOrders.isEmpty() && isEnoughAssetBalance(USDT, enoughAssetBalance)) 
        {
            double enterPrice = findEnterPrice();
            String orderId = createBuyOrder(enterPrice);
            logger.info(BUY_ORDER_MSG + orderId);
            panicStrategyService.discard();
        } 
        else if (openOrders.isEmpty() && isEnoughAssetBalance(currentCoin, Double.parseDouble(buyQuantity))) 
        {
            double enterPrice = findExitPrice();
            String orderId = createSellOrder(enterPrice);
            logger.info(SELL_ORDER_MSG + orderId);
        } 
        else if (currentOrder != null && BUY_ORDER_SIDE.equals(currentOrder.side())) 
        {
            if (checkIfNeedExit(currentOrder.price())) 
            {
                cancelOrder(currentOrder.clientOrderId());
            }
        } 
        else if (currentOrder != null && SELL_ORDER_SIDE.equals(currentOrder.side())) 
        {
            if (checkIfNeedExit(currentOrder.price())) 
            {
                panicStrategyService.incrementSellCount();
                cancelOrder(currentOrder.clientOrderId());
            }
        } 
        else 
        {
            logger.info(UNEXPECTED_MSG + currentOrder);
        }
    }

    public String panicSell() 
    {

        logger.info(">>> EMERGENCY SELL " + getAssetBalanceForCurrency(USDT));
        return orderService.createMarketSell(buyQuantity);
    }

    public void cancelCurrentOrders() 
    {

        logger.info(">>> Cancel All Current Orders");
        List<ClientOrder> openOrders = getOpenOrders();
        if (!openOrders.isEmpty()) 
        {
            openOrders.forEach(order -> cancelOrder(order.clientOrderId()));
        } 
        else 
        {
            logger.info(">>> No opened orders found");
        }
    }

    private void cancelOrder(final String clientOrderId) 
    {

        orderService.cancelOrder(currentTradePair, clientOrderId);
    }

    private boolean checkIfNeedExit(final String currentPrice) 
    {

        double averagePrice = trendCache.getAverage().orElseThrow(CacheException::new);
        double orderPrice = parseDouble(currentPrice);
        return Math.abs((averagePrice - orderPrice) * 100D / averagePrice) > exitLevel;
    }

    private String createBuyOrder(double enterPrice) 
    {

        String formatPrice = Converter.convertToStringDecimal(enterPrice);
        return orderService.createLimitBuy(buyQuantity, formatPrice);
    }

    private String createSellOrder(double enterPrice) 
    {

        String formatPrice = Converter.convertToStringDecimal(enterPrice);
        return orderService.createLimitSell(buyQuantity, formatPrice);
    }

    private List<ClientOrder> getOpenOrders() 
    {

        return orderService.getOpenOrders(currentTradePair)
                .stream()
                .filter(Objects::nonNull)
                .map(ordersMapper::toRecord)
                .collect(Collectors.toList());
    }

    private double findEnterPrice() 
    {

        double enterPrice = trendCache.getAverage().orElseThrow(CacheException::new);
        enterPrice *= enterPriceParam;
        return enterPrice;
    }

    private double findExitPrice() 
    {

        double exitPrice = trendCache.getAverage().orElseThrow(CacheException::new);
        exitPrice *= exitPriceParam;
        return exitPrice;
    }

    private boolean isEnoughAssetBalance(final String currency, final Double limit) 
    {

        String freeBalanceValue = getAssetBalanceFor(currency).free();
        return Double.parseDouble(freeBalanceValue) > limit;
    }

    private double getAssetBalanceForCurrency(final String currency) 
    {

        String freeBalanceValue = getAssetBalanceFor(currency).free();
        return Double.parseDouble(freeBalanceValue);
    }

    private boolean canEnter() 
    {

        return trendCache.isComfy();
    }
}
