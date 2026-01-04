package model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "merchants")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Merchant {

    @Id
    @Column(name = "merchant_id", unique = true, nullable = false)
    private String merchantId; // id koji mi dodjeljujemo prodavcu (npr. "rent-a-car-ns")

    @Column(name = "merchant_password", nullable = false)
    private String merchantPassword;

    @Column(name = "name")
    private String name;

    @Column(name = "web_shop_url")
    private String webShopUrl;

}