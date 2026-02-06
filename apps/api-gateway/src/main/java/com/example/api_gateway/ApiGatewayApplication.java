package com.example.api_gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates;
import org.springframework.context.annotation.Bean;
import org.springframework.cloud.netflix.eureka.http.EurekaClientHttpRequestFactorySupplier;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;

import javax.net.ssl.SSLContext;

import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions;
import org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions;

import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.path;

import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import java.net.URI;

@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }

    @Bean
    public EurekaClientHttpRequestFactorySupplier eurekaClientHttpRequestFactorySupplier() {
        return (sslContext, hostnameVerifier) -> {
            SSLContext contextToUse = (sslContext != null) ? sslContext : getSystemSslContext();

            var socketFactory = SSLConnectionSocketFactoryBuilder.create()
                    .setSslContext(contextToUse)
                    .setHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                    .build();

            var connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                    .setSSLSocketFactory(socketFactory)
                    .build();

            var httpClient = HttpClients.custom()
                    .setConnectionManager(connectionManager)
                    .build();

            return new HttpComponentsClientHttpRequestFactory(httpClient);
        };
    }

    @Bean
    public RestClient.Builder restClientBuilder() {
        try {
            // Kreiramo TrustManager koji veruje svim sertifikatima
            javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[]{
                    new javax.net.ssl.X509TrustManager() {
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }

                        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                        }

                        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                        }
                    }
            };

            javax.net.ssl.SSLContext sslContext = javax.net.ssl.SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            var socketFactory = org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder.create()
                    .setSslContext(sslContext)
                    .setHostnameVerifier(org.apache.hc.client5.http.ssl.NoopHostnameVerifier.INSTANCE)
                    .build();

            var connectionManager = org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder.create()
                    .setSSLSocketFactory(socketFactory)
                    .build();

            var httpClient = org.apache.hc.client5.http.impl.classic.HttpClients.custom()
                    .setConnectionManager(connectionManager)
                    .build();

            return RestClient.builder()
                    .requestFactory(new org.springframework.http.client.HttpComponentsClientHttpRequestFactory(httpClient));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private SSLContext getSystemSslContext() {
        try {
            return SSLContext.getDefault();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}