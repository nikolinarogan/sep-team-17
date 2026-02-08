package dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyMfaRequestDTO {
    @NotBlank(message = "Username (email) is required")
    private String username;

    @NotBlank(message = "Verification code is required")
    private String code;
}
