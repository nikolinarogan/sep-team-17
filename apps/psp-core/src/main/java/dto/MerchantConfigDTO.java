package dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@NoArgsConstructor
public class MerchantConfigDTO {
    @NotBlank
    private String methodName;

    // Kredencijali za taj metod (npr. username/password za banku)
    // Key-Value parovi koje ćemo pretvoriti u JSON
    private Map<String, String> credentials;
}
