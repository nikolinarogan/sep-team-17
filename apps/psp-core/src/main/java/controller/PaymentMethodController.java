package controller;

import dto.PaymentMethodRequestDTO;
import model.PaymentMethod;
import repository.PaymentMethodRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payment-methods")
@CrossOrigin(origins = "*")
public class PaymentMethodController {

    private final PaymentMethodRepository paymentMethodRepository;

    public PaymentMethodController(PaymentMethodRepository paymentMethodRepository) {
        this.paymentMethodRepository = paymentMethodRepository;
    }

    // Vraća sve dostupne metode
    @GetMapping
    public ResponseEntity<List<PaymentMethod>> getAllMethods() {
        return ResponseEntity.ok(paymentMethodRepository.findAll());
    }

    // Dodaje novi globalni metod (npr. CRYPTO)
    @PostMapping
    public ResponseEntity<PaymentMethod> createMethod(@RequestBody PaymentMethodRequestDTO request) {
        if (paymentMethodRepository.findByName(request.getName()).isPresent()) {
            throw new RuntimeException("Metod " + request.getName() + " već postoji.");
        }

        PaymentMethod pm = new PaymentMethod();
        pm.setName(request.getName());
        pm.setServiceUrl(request.getServiceUrl());

        return ResponseEntity.ok(paymentMethodRepository.save(pm));
    }

    // Brisanje metoda
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMethod(@PathVariable Long id) {
        paymentMethodRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}