package dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
public class CheckoutResponseDTO {
    private BigDecimal amount;
    private String currency;
    private String merchantId;
    private List<String> availableMethods;
}
