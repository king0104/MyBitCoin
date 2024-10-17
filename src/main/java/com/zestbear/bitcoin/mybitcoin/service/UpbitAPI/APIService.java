package com.zestbear.bitcoin.mybitcoin.service.UpbitAPI;

import com.zestbear.bitcoin.mybitcoin.service.UpbitAPI.Account.AccountAPI;
import com.zestbear.bitcoin.mybitcoin.service.UpbitAPI.Candle.CurrentValueAPI;
import com.zestbear.bitcoin.mybitcoin.service.UpbitAPI.Candle.MinuteCandleAPI;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j; // 추가
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

@Slf4j // 추가
@Service
public class APIService {

    private final AccountAPI accountAPI;
    private final CurrentValueAPI currentValueAPI;
    private final MinuteCandleAPI minuteCandleAPI;

    @Autowired
    public APIService(AccountAPI accountAPI, CurrentValueAPI currentValueAPI, MinuteCandleAPI minuteCandleAPI) {
        this.accountAPI = accountAPI;
        this.currentValueAPI = currentValueAPI;
        this.minuteCandleAPI = minuteCandleAPI;
    }

    @PostConstruct
    public void init() {
        try {
            log.info("Initializing APIService"); // 로그 추가
            updateAccount();
            updateCurrentValue();
            updateMinuteCandle();
        } catch (RuntimeException e) {
            log.error("Error during initialization", e); // 로그 추가
            throw new RuntimeException(e);
        }
    }

    /*
        내 계좌의 정보를 불러오는 API
     */
    public synchronized void updateAccount() {
        try {
            log.info("Updating account data"); // 로그 추가
            accountAPI.getAccountsAPI();
            log.info("Account data updated"); // 로그 추가
        } catch (ExecutionException | InterruptedException e) {
            log.error("Error updating account data", e);
            throw new RuntimeException(e);
        }
    }

    /*
        현재 시세를 얻어오는 API
     */
    public synchronized void updateCurrentValue() {
        try {
            log.info("Updating current value data"); // 로그 추가
            currentValueAPI.getCurrentValueAPI();
            log.info("Current value data updated"); // 로그 추가
        } catch (IOException | ExecutionException | InterruptedException e) {
            log.error("Error updating current value data", e);
            throw new RuntimeException(e);
        }
    }

    /*
        이전 200개의 분봉을 얻어오는 API
     */
    public synchronized void updateMinuteCandle() {
        try {
            log.info("Updating minute candle data"); // 로그 추가
            minuteCandleAPI.getMinuteCandleAPI();
            log.info("Minute candle data updated"); // 로그 추가
        } catch (ExecutionException | InterruptedException e) {
            log.error("Error updating minute candle data", e);
            throw new RuntimeException(e);
        }
    }
}
