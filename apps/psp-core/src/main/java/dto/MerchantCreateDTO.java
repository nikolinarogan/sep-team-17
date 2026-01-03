package dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MerchantCreateDTO {
    private String name;
    private String webShopUrl;
}