package repository;

import model.PspConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PspConfigRepository extends JpaRepository<PspConfig, String> {
    Optional<PspConfig> findByConfigName(String configName);
}