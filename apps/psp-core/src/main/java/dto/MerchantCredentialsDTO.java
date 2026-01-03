package dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MerchantCredentialsDTO {
    private String merchantId;       // Javno (API Key)
    private String merchantPassword; // Tajno (API Secret)
}