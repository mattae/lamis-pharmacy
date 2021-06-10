package org.fhi360.lamis.modules.pharmacy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fhi360.lamis.modules.patient.service.providers.PatientObservationViewProvider;
import org.lamisplus.modules.lamis.legacy.domain.entities.Patient;
import org.lamisplus.modules.lamis.legacy.domain.repositories.PharmacyRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class TPTCompletionObservationProvider implements PatientObservationViewProvider {
    private final PharmacyService pharmacyService;
    private final PharmacyRepository pharmacyRepository;

    @Override
    public boolean applicableTo(Patient patient) {
        LocalDate lastTpt = pharmacyService.dateOfLastIptBefore(patient.getId(), LocalDate.now());
        if (lastTpt != null && !lastTpt.plusMonths(6).isAfter(LocalDate.now())) {
            return pharmacyService.hasUncompletedIptAfter(patient.getId(), lastTpt) &&
                !pharmacyRepository.findByPatientAndDateVisitAfter(patient, lastTpt.plusMonths(2).minusDays(1),
                    PageRequest.of(0, Integer.MAX_VALUE)).isEmpty();
        }
        return false;
    }

    @Override
    public String getName() {
        return "Complete TPT";
    }

    @Override
    public String getPath() {
        return "tpt-completion";
    }
}
