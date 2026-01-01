package dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PaymentMethodDTO {
    private String name;       // npr. "CARD"
    private String serviceUrl; // npr. "http://localhost:8082/api/card" - URL Plugin-a
}
