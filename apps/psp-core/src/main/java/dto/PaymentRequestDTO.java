package dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentRequestDTO {
    @NotBlank(message = "Merchant ID je obavezan")
    private String merchantId;

    @NotBlank(message = "Merchant Password je obavezan")
    private String merchantPassword;

    @NotNull(message = "Iznos je obavezan")
    @DecimalMin(value = "0.1", message = "Iznos mora biti veći od nule")
    private BigDecimal amount;

    @NotBlank(message = "Valuta je obavezna")
    @Size(min = 3, max = 3)
    private String currency;

    @NotBlank(message = "ID narudžbine prodavca je obavezan")
    private String merchantOrderId;

    private LocalDateTime merchantTimestamp;

    @NotBlank(message = "Success URL je obavezan")
    private String successUrl;

    @NotBlank(message = "Failed URL je obavezan")
    private String failedUrl;

    @NotBlank(message = "Error URL je obavezan")
    private String errorUrl;
}
