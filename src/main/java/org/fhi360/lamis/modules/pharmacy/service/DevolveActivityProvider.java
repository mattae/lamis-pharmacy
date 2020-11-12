package org.fhi360.lamis.modules.pharmacy.service;

import lombok.RequiredArgsConstructor;
import org.fhi360.lamis.modules.patient.service.providers.PatientActivityProvider;
import org.fhi360.lamis.modules.patient.service.providers.vm.PatientActivity;
import org.lamisplus.modules.lamis.legacy.domain.entities.Patient;
import org.lamisplus.modules.lamis.legacy.domain.repositories.DevolveRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DevolveActivityProvider implements PatientActivityProvider {
    private final DevolveRepository devolveRepository;

    @Override
    public List<PatientActivity> getActivitiesFor(Patient patient) {
        List<PatientActivity> activities = new ArrayList<>();
        devolveRepository.findByPatient(patient).forEach(devolve -> {
            PatientActivity activity = new PatientActivity(devolve.getUuid(), "Client Devolvement", devolve.getDateDevolved(),
                    "", "devolves");
            activities.add(activity);

            if (devolve.getDateReturnedToFacility() != null) {
                activity = new PatientActivity(devolve.getUuid(), "Client Returns to Facility", devolve.getDateReturnedToFacility(),
                        "", "devolves");
                activities.add(activity);
            }
        });
        return activities;
    }
}
