package com.bank.config;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient() {
        try {
            // OVO REŠAVA TVOJ NOVI PROBLEM "No name matching localhost"
            // InsecureTrustManagerFactory radi dve stvari:
            // 1. Veruje sertifikatu (rešava PKIX - mada ti to već radiš preko truststore-a)
            // 2. IGNORIŠE PROVERU IMENA (Hostname Verification) - Ovo ti sada treba!

            SslContext sslContext = SslContextBuilder
                    .forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .build();

            HttpClient httpClient = HttpClient.create()
                    .secure(t -> t.sslContext(sslContext));

            return WebClient.builder()
                    .clientConnector(new ReactorClientHttpConnector(httpClient))
                    .build();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
