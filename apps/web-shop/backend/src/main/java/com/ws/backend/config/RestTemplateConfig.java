package com.ws.backend.config;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.ssl.SSLContexts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;

@Configuration
public class RestTemplateConfig {
    @Value("${server.ssl.trust-store}")
    private Resource trustStore;

    @Value("${server.ssl.trust-store-password}")
    private String trustStorePassword;

    /*@Bean
    public RestTemplate restTemplate() throws Exception {
        // 1. Kreiramo SSLContext koji veruje
        SSLContext sslContext = SSLContexts.custom()
                .loadTrustMaterial(null, TrustAllStrategy.INSTANCE)
                .build();

        // 2. Pravimo HttpClient koji koristi taj SSLContext
        HttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setSSLSocketFactory(SSLConnectionSocketFactoryBuilder.create()
                        .setSslContext(sslContext)
                        // BITNO: Ovde isključujemo proveru Hostname-a
                        .setHostnameVerifier((hostname, session) -> true)
                        .build())
                .build();

        // 3. Kreiramo klijenta
        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .build();

        // 4. Ubacujemo klijenta u RestTemplate
        return new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient));

    }*/
    @Bean
    public RestTemplate restTemplate() throws Exception {
        // 1. SSL Context (Ovo ti je dobro)
        SSLContext sslContext = SSLContexts.custom()
                .loadTrustMaterial(null, TrustAllStrategy.INSTANCE)
                .build();

        // 2. HttpClient sa eksplicitnim forsiranjem HTTP/1.1
        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(PoolingHttpClientConnectionManagerBuilder.create()
                        .setSSLSocketFactory(SSLConnectionSocketFactoryBuilder.create()
                                .setSslContext(sslContext)
                                .setHostnameVerifier((hostname, session) -> true)
                                .build())
                        .build())
                // DODAJ OVO: Forsiraj HTTP/1.1 da izbegneš chunked/h2 probleme sa Gateway-om
                .setConnectionReuseStrategy((request, response, context) -> true)
                .build();

        // 3. Postavi request factory
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);



        return new RestTemplate(factory);
    }
}
