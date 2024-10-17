package com.zestbear.bitcoin.mybitcoin.service.Execute;

import com.zestbear.bitcoin.mybitcoin.service.Strategy.GetRatio;
import com.zestbear.bitcoin.mybitcoin.service.Strategy.MAComparison;
import com.zestbear.bitcoin.mybitcoin.service.Strategy.RSICalculator;
import com.zestbear.bitcoin.mybitcoin.service.Strategy.LossRatio;
import com.zestbear.bitcoin.mybitcoin.service.UpbitAPI.Account.CurrentAsset;
import com.zestbear.bitcoin.mybitcoin.service.UpbitAPI.Candle.CurrentValueAPI;
import com.zestbear.bitcoin.mybitcoin.service.UpbitAPI.Order.OrderAPI;
import lombok.extern.slf4j.Slf4j; // 추가
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

@Slf4j // 추가
@Service
public class OrderService {

    private final CurrentAsset currentAsset;
    private final OrderAPI orderAPI;
    private final CurrentValueAPI currentValueAPI;
    private final MAComparison maComparison;
    private final RSICalculator rsiCalculator;
    private final LossRatio lossRatio;
    private final GetRatio getRatio;

    public OrderService(CurrentAsset currentAsset, OrderAPI orderAPI, CurrentValueAPI currentValueAPI, MAComparison maComparison, RSICalculator rsiCalculator, LossRatio lossRatio, GetRatio getRatio) {
        this.currentAsset = currentAsset;
        this.orderAPI = orderAPI;
        this.currentValueAPI = currentValueAPI;
        this.maComparison = maComparison;
        this.rsiCalculator = rsiCalculator;
        this.lossRatio = lossRatio;
        this.getRatio = getRatio;
    }

    public void sendOrder() throws IOException, NoSuchAlgorithmException {
        String[] coinSymbols = {"BTC", "ETH", "ETC", "SOL", "DOT"};
        double cashKRW = currentAsset.getCashKRW();                             // 현금 보유량
        Map<String, Double> eachValues = currentAsset.getEachValue();           // 보유량 * 시장가
        Map<String, Double> currentValues = currentValueAPI.getCurrentValues(); // 시장가

        log.info("Current cashKRW: {}", cashKRW); // 로그 추가
        log.info("Each coin values: {}", eachValues); // 로그 추가
        log.info("Current market prices: {}", currentValues); // 로그 추가

        for (String symbol : coinSymbols) {

            log.info("Processing symbol: {}", symbol); // 로그 추가

            if (!eachValues.containsKey(symbol)) {
                String maTiming = maComparison.isMATiming(symbol);
                double rsiValue = rsiCalculator.getCalculatedRSI(symbol);
                log.info("MA Timing for {}: {}", symbol, maTiming); // 로그 추가
                log.info("RSI Value for {}: {}", symbol, rsiValue); // 로그 추가
                if ((maTiming.equals("bid") && rsiValue < 25) || rsiValue < 15) {
                    if (cashKRW >= 30000) {
                        String price = String.valueOf(30000);
                        log.info("Placing buy order for {}: {}", symbol, price); // 로그 추가
                        orderAPI.postOrder("bid", "KRW-" + symbol, price, null);
                    }
                }
            }

            if (eachValues.containsKey(symbol)) {
                double gainRatio = getRatio.GetNow(symbol);
                double rsiValue = rsiCalculator.getCalculatedRSI(symbol);
                log.info("Gain ratio for {}: {}", symbol, gainRatio); // 로그 추가
                log.info("RSI Value for {}: {}", symbol, rsiValue); // 로그 추가
                if (gainRatio >= 0.005) {
                    if ((maComparison.isMATiming(symbol).equals("ask") && rsiValue > 70) || lossRatio.isLoss(symbol) || gainRatio >= 0.035 || rsiValue > 83) {
                        double volume = eachValues.get(symbol) / currentValues.get("KRW-" + symbol);
                        log.info("Placing sell order for {}: Volume {}", symbol, volume); // 로그 추가
                        orderAPI.postOrder("ask", "KRW-" + symbol, null, String.format("%.8f", volume));
                    }
                }

                if (eachValues.get(symbol) < 50000 && rsiValue < 11 && cashKRW >= 10000) {
                    String price = String.valueOf(10000);
                    log.info("Placing additional buy order for {}: {}", symbol, price); // 로그 추가
                    orderAPI.postOrder("bid", "KRW-" + symbol, price, null);
                }
            }
        }
    }
}
