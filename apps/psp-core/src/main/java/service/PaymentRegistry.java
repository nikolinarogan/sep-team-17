package service;

import exception.UnknownPaymentmethodException;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry svih payment handler-a. Spring automatski injektuje sve bean-ove
 * tipa PaymentHandler. Nova metoda = novi handler sa @Component, bez izmene ovde.
 */
@Component
public class PaymentRegistry {

    private final Map<String, PaymentProvider> handlers = new HashMap<>();

    public PaymentRegistry(List<PaymentProvider> allHandlers) {
        for (PaymentProvider h : allHandlers) {
            handlers.put(h.getProviderName(), h);
        }
    }

    public PaymentProvider get(String methodName) {
        PaymentProvider h = handlers.get(methodName);
        if (h == null) {
            throw new UnknownPaymentmethodException(methodName);
        }
        return h;
    }

    public boolean hasMethod(String methodName) {
        return handlers.containsKey(methodName);
    }
}