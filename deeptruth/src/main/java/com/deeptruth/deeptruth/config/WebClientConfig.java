package com.deeptruth.deeptruth.config;

import io.netty.channel.ChannelOption;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;import reactor.netty.http.client.HttpClient;

import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfig {
    @Bean
    public WebClient webClient() {

        HttpClient httpClient = HttpClient.create()
                // 연결 타임아웃 (TCP connect)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)       // 10초
                // 서버 응답 헤더 받을 때까지의 타임아웃
                .responseTimeout(Duration.ofMinutes(10))                   // 120초
                // 데이터 송수신 타임아웃(스트림 단계)
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(600, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(600, TimeUnit.SECONDS))
                );

        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(c -> c.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB
                .build();

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(strategies).
                build();
    }
}