package org.fhi360.lamis.modules.pharmacy.service;

import lombok.AllArgsConstructor;
import org.fhi360.lamis.modules.patient.service.providers.PatientObservationViewProvider;
import org.lamisplus.modules.lamis.legacy.domain.entities.Patient;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class PharmacyRefillObservationProvider implements PatientObservationViewProvider {

    @Override
    public boolean applicableTo(Patient patient) {
        return true;
    }

    @Override
    public String getName() {
        return "Pharmacy Refill";
    }

    @Override
    public String getPath() {
        return "pharmacies";
    }

    @Override
    public String getTooltip() {
        return "New Pharmacy Refill";
    }

    @Override
    public String getIcon() {
        return null;
    }
}
