package com.example.api_gateway;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class PspProxyController {

    private final RestClient.Builder restClientBuilder;
    private final DiscoveryClient discoveryClient;

    private final AtomicInteger requestCounter = new AtomicInteger(0);

    public PspProxyController(RestClient.Builder restClientBuilder, DiscoveryClient discoveryClient) {
        this.restClientBuilder = restClientBuilder;
        this.discoveryClient = discoveryClient;
    }

    // Promenjeno sa /api/payments/** na /api/** da bi hvatali sve rute (admin, merchants, itd.)
    @RequestMapping(value = "/api/**")
    public ResponseEntity<?> proxyRequest(HttpServletRequest request, @RequestBody(required = false) byte[] body) {
        // 1. Pronalaženje servisa
        List<ServiceInstance> instances = discoveryClient.getInstances("PSP-CORE");

        if (instances.isEmpty()) {
            return ResponseEntity.status(503).body("PSP-CORE servis nije dostupan na Eureki");
        }

        // --- ROUND ROBIN LOGIKA ---
        int currentCount = requestCounter.getAndIncrement();
        int instanceIndex = Math.abs(currentCount) % instances.size();

        ServiceInstance instance = instances.get(instanceIndex);

        String path = request.getRequestURI();
        System.out.println("GATEWAY -> " + request.getMethod() + " " + path + " prosleđujem na instancu port: " + instance.getPort());

        String baseUrl = instance.getUri().toString();
        String query = request.getQueryString();
        String fullPath = path + (query != null ? "?" + query : "");

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
                if (connection instanceof HttpsURLConnection) {
                    HttpsURLConnection httpsConnection = (HttpsURLConnection) connection;

                    try {
                        TrustManager[] trustAllCerts = new TrustManager[]{
                                new X509TrustManager() {
                                    public X509Certificate[] getAcceptedIssuers() { return null; }
                                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                                }
                        };
                        SSLContext sc = SSLContext.getInstance("TLS");
                        sc.init(null, trustAllCerts, new java.security.SecureRandom());
                        httpsConnection.setSSLSocketFactory(sc.getSocketFactory());

                        httpsConnection.setHostnameVerifier((hostname, session) -> true);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                super.prepareConnection(connection, httpMethod);
                connection.setInstanceFollowRedirects(false);
            }
        };

//        return restClientBuilder
//                .requestFactory(factory)
//                .build()
//                .method(HttpMethod.valueOf(request.getMethod()))
//                .uri(baseUrl + fullPath)
//                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
//                .body(body != null ? body : new byte[0])
//                .exchange((req, res) -> {
//                    org.springframework.http.HttpHeaders filteredHeaders = new org.springframework.http.HttpHeaders();
//                    res.getHeaders().forEach((headerName, headerValues) -> {
//                        if (!headerName.equalsIgnoreCase("Transfer-Encoding")) {
//                            filteredHeaders.addAll(headerName, headerValues);
//                        }
//                    });
//
//                    return ResponseEntity.status(res.getStatusCode())
//                            .headers(filteredHeaders)
//                            .body(res.getBody().readAllBytes());
//                });
        HttpHeaders forwardHeaders = copyForwardHeaders(request);
        if (forwardHeaders.getContentType() == null && body != null && body.length > 0) {
            forwardHeaders.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        }

        var spec = restClientBuilder
                .requestFactory(factory)
                .build()
                .method(HttpMethod.valueOf(request.getMethod()))
                .uri(baseUrl + fullPath)
                .headers(h -> h.addAll(forwardHeaders));

        if (body != null && body.length > 0) {
            spec.body(body);
        } else if (!"GET".equalsIgnoreCase(request.getMethod()) && !"HEAD".equalsIgnoreCase(request.getMethod())) {
            spec.body(new byte[0]);
        }

        return spec.exchange((req, res) -> {
            org.springframework.http.HttpHeaders filteredHeaders = new org.springframework.http.HttpHeaders();
            res.getHeaders().forEach((headerName, headerValues) -> {
                if (!headerName.equalsIgnoreCase("Transfer-Encoding")) {
                    filteredHeaders.addAll(headerName, headerValues);
                }
            });
            return ResponseEntity.status(res.getStatusCode())
                    .headers(filteredHeaders)
                    .body(res.getBody().readAllBytes());
        });
    }

    private HttpHeaders copyForwardHeaders(HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        Enumeration<String> names = request.getHeaderNames();
        if (names != null) {
            while (names.hasMoreElements()) {
                String name = names.nextElement();
                if (!name.equalsIgnoreCase("Host") && !name.equalsIgnoreCase("Content-Length")
                        && !name.equalsIgnoreCase("Transfer-Encoding") && !name.equalsIgnoreCase("Connection")) {
                    Enumeration<String> values = request.getHeaders(name);
                    while (values.hasMoreElements()) {
                        headers.add(name, values.nextElement());
                    }
                }
            }
        }
        return headers;
    }
}