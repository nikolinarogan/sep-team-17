package com.ws.backend.service;
import com.ws.backend.model.Insurance;
import com.ws.backend.model.InsuranceType;
import com.ws.backend.repository.InsuranceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class InsuranceService {

    @Autowired
    private InsuranceRepository insuranceRepository;

    public Insurance addInsurance (Double pricePerDay, InsuranceType type, Boolean isAvailable) throws IOException {
        Insurance insurance = new Insurance();
        insurance.setType(type);
        insurance.setPrice(pricePerDay);
        insurance.setIsAvailable(isAvailable != null ? isAvailable : true);

        return insuranceRepository.save(insurance);
    }

    public Insurance getInsuranceById(Long id) {
        return insuranceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Insurance not found with id: " + id));
    }

    public List<Insurance> getAllInsurances() {
        return insuranceRepository.findAll();
    }

    public List<Insurance> getAvailableInsurances() {
        return insuranceRepository.findByIsAvailableTrue();
    }

    public Insurance updateInsurance(Long id, Insurance updatedInsurance) throws IOException {
        Insurance insurance = getInsuranceById(id);
        insurance.setType(updatedInsurance.getType());
        insurance.setPrice(updatedInsurance.getPrice());
        insurance.setIsAvailable(updatedInsurance.getIsAvailable());

        return insuranceRepository.save(insurance);
    }

    public void deleteInsurance(Long id) {
        Insurance insurance = getInsuranceById(id);
        insuranceRepository.delete(insurance);
    }
}
