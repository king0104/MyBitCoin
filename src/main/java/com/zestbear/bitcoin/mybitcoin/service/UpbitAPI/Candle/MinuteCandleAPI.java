package com.zestbear.bitcoin.mybitcoin.service.UpbitAPI.Candle;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zestbear.bitcoin.mybitcoin.dto.MinuteCandleDto;
import lombok.extern.slf4j.Slf4j; // 추가
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

@Slf4j // 추가
@Service
public class MinuteCandleAPI {

    private final Map<String, List<MinuteCandleDto>> minuteCandles = new ConcurrentHashMap<>();

    public void getMinuteCandleAPI() throws ExecutionException, InterruptedException {

        log.info("Starting getMinuteCandleAPI"); // 로그 추가

        String[] coinSymbols = {"BTC", "ETH", "ETC", "SOL", "DOT"};
        String urlTemplate = "https://api.upbit.com/v1/candles/minutes/5?market=KRW-%s&count=200";
        ObjectMapper objectMapper = new ObjectMapper();
        CloseableHttpClient httpClient = HttpClients.createDefault();

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (String coinSymbol : coinSymbols) {
            String url = String.format(urlTemplate, coinSymbol);
            HttpGet httpGet = new HttpGet(url);

            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                    int statusCode = response.getStatusLine().getStatusCode();

                    if (statusCode == 200) {  // 정상적인 응답
                        HttpEntity entity = response.getEntity();
                        if (entity != null) {
                            String result;
                            try {
                                result = EntityUtils.toString(entity);

                                log.info("Received minute candle data for {}: {}", coinSymbol, result); // 로그 추가

                                MinuteCandleDto[] minuteCandleData =
                                        objectMapper.readValue(result, MinuteCandleDto[].class);
                                minuteCandles.put(coinSymbol, Arrays.asList(minuteCandleData));
                            } catch (IOException e) {
                                log.error("Error parsing minute candle data for {}", coinSymbol, e); // 로그 추가
                                throw new RuntimeException(e);
                            }
                        }
                    } else if (statusCode == 429) {  // Too Many Requests
                        log.error("Too many API requests for symbol: {} In MINUTE", coinSymbol); // 로그 수정
                    } else {
                        log.error("Unexpected status code: {}, for symbol: {}", statusCode, coinSymbol); // 로그 수정
                    }
                } catch (IOException e) {
                    log.error("Exception in getMinuteCandleAPI for symbol: {}", coinSymbol, e); // 로그 추가
                    throw new RuntimeException(e);
                }
            });

            futures.add(future);
        }

        // 모든 Future가 완료될 때까지 대기
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();

        log.info("Completed getMinuteCandleAPI"); // 로그 추가
    }

    public Map<String, List<MinuteCandleDto>> getMinuteCandles() {
        return minuteCandles;
    }
}
