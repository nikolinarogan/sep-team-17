package controller;

import dto.MerchantConfigDTO;
import service.MerchantService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/merchants")
@CrossOrigin(origins = "*")
public class MerchantController {

    private final MerchantService merchantService;

    public MerchantController(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    /**
     * Endpoint za dodavanje/ažuriranje servisa prodavca
     * PUT /api/merchants/{id}/services
     */
    @PutMapping("/{merchantId}/services")
    public ResponseEntity<String> updateServices(@PathVariable String merchantId,
                                                 @RequestBody List<MerchantConfigDTO> configs) {
        merchantService.updateMerchantServices(merchantId, configs);
        return ResponseEntity.ok("Servisi uspješno ažurirani.");
    }

    /**
     * Endpoint za uklanjanje servisa
     * DELETE /api/merchants/{id}/services/{methodName}
     */
    @DeleteMapping("/{merchantId}/services/{methodName}")
    public ResponseEntity<String> removeService(@PathVariable String merchantId,
                                                @PathVariable String methodName) {
        merchantService.removeMerchantService(merchantId, methodName);
        return ResponseEntity.ok("Servis uspješno uklonjen.");
    }
}