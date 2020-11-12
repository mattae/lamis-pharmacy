package org.fhi360.lamis.modules.pharmacy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fhi360.lamis.modules.patient.service.providers.PatientActivityProvider;
import org.fhi360.lamis.modules.patient.service.providers.vm.PatientActivity;
import org.lamisplus.modules.lamis.legacy.domain.entities.Clinic;
import org.lamisplus.modules.lamis.legacy.domain.entities.Patient;
import org.lamisplus.modules.lamis.legacy.domain.entities.Pharmacy;
import org.lamisplus.modules.lamis.legacy.domain.repositories.PatientRepository;
import org.lamisplus.modules.lamis.legacy.domain.repositories.PharmacyRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefillActivityProvider implements PatientActivityProvider {
    private final PharmacyRepository pharmacyRepository;
    private final PatientRepository patientRepository;

    @Override
    public List<PatientActivity> getActivitiesFor(Patient patient) {
        List<PatientActivity> activities = new ArrayList<>();
        patient = patientRepository.getOne(patient.getId());
        List<Pharmacy> clinics = pharmacyRepository.findByPatient(patient);
        clinics.forEach(pharmacy -> {
            String name = "Pharmacy Refill";
            PatientActivity activity = new PatientActivity(pharmacy.getUuid(), name, pharmacy.getDateVisit(), "", "pharmacies");
            activities.add(activity);
        });
        return activities;
    }
}
