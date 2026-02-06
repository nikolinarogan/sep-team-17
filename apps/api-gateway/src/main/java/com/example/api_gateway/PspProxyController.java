package com.example.api_gateway;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import java.util.List;

@RestController
public class PspProxyController {

    private final RestClient.Builder restClientBuilder;
    private final DiscoveryClient discoveryClient;

    public PspProxyController(RestClient.Builder restClientBuilder, DiscoveryClient discoveryClient) {
        this.restClientBuilder = restClientBuilder;
        this.discoveryClient = discoveryClient;
    }

    @RequestMapping(value = "/api/payments/**")
    public ResponseEntity<?> proxyRequest(HttpServletRequest request, @RequestBody(required = false) byte[] body) {
        List<ServiceInstance> instances = discoveryClient.getInstances("PSP-CORE");

        if (instances.isEmpty()) {
            return ResponseEntity.status(503).body("PSP-CORE servis nije dostupan na Eureki");
        }

        ServiceInstance instance = instances.get(0);
        String baseUrl = instance.getUri().toString();

        String path = request.getRequestURI();
        String query = request.getQueryString();
        String fullPath = path + (query != null ? "?" + query : "");

        return restClientBuilder.build()
                .method(HttpMethod.valueOf(request.getMethod()))
                .uri(baseUrl + fullPath)
                .body(body != null ? body : new byte[0])
                .exchange((req, res) -> {
                    return ResponseEntity.status(res.getStatusCode())
                            .headers(res.getHeaders())
                            .body(res.getBody().readAllBytes());
                });
    }
}