package com.zestbear.bitcoin.mybitcoin.service.UpbitAPI.Candle;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zestbear.bitcoin.mybitcoin.dto.CurrentDataDto;
import lombok.extern.slf4j.Slf4j; // 추가
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

@Slf4j // 추가
@Service
public class CurrentValueAPI {

    private final Map<String, Double> CurrentValues = new ConcurrentHashMap<>();  // 각 코인들의 시장가

    public void getCurrentValueAPI() throws IOException, ExecutionException, InterruptedException {

        log.info("Starting getCurrentValueAPI"); // 로그 추가

        OkHttpClient client = new OkHttpClient();
        String[] coinSymbols = {"BTC", "ETH", "ETC", "SOL", "DOT"};
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (String coin : coinSymbols) {
            String url = String.format("https://api.upbit.com/v1/ticker?markets=KRW-%s", coin);

            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                Request request = new Request.Builder()
                        .url(url)
                        .get()
                        .addHeader("accept", "application/json")
                        .build();

                try (Response response = client.newCall(request).execute()) {

                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();

                        log.info("Received response for {}: {}", coin, responseBody); // 로그 추가

                        ObjectMapper mapper = new ObjectMapper();
                        CurrentDataDto[] currentDatas = mapper.readValue(responseBody, CurrentDataDto[].class);

                        for (CurrentDataDto currentData : currentDatas) {
                            CurrentValues.put(currentData.getMarket(), currentData.getTrade_price());
                        }
                    } else {
                        log.error("Error: {} for coin: {}", response.code(), coin); // 로그 추가
                    }
                } catch (IOException e) {
                    log.error("Exception in getCurrentValueAPI for coin: {}", coin, e); // 로그 추가
                    throw new RuntimeException(e);
                }
            });

            futures.add(future);
        }

        // 모든 Future가 완료될 때까지 대기
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();

        log.info("Completed getCurrentValueAPI with data: {}", CurrentValues); // 로그 추가
    }

    public Map<String, Double> getCurrentValues() {
        return CurrentValues;
    }
}
