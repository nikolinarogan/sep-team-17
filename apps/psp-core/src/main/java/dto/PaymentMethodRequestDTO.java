package dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PaymentMethodRequestDTO {
    private String name;       // npr. "CRYPTO"
    private String serviceUrl; // npr. "http://localhost:8085/api/crypto"
}