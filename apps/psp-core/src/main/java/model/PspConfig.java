package model;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "psp_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PspConfig {
    @Id
    @Column(name = "config_name")
    private String configName; // npr. "FRONTEND_URL"

    @Column(name = "config_value")
    private String configValue; // npr. "http://localhost:4201"
}
