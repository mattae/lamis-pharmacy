package org.fhi360.lamis.modules.pharmacy.service;

import lombok.RequiredArgsConstructor;
import org.fhi360.lamis.modules.patient.service.providers.PatientObservationViewProvider;
import org.lamisplus.modules.lamis.legacy.domain.entities.Devolve;
import org.lamisplus.modules.lamis.legacy.domain.entities.Patient;
import org.lamisplus.modules.lamis.legacy.domain.repositories.DevolveRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EndDevolveObservationProvider implements PatientObservationViewProvider {
    private final DevolveRepository devolveRepository;

    @Override
    public boolean applicableTo(Patient patient) {
        List<Devolve> devolves = devolveRepository.findByPatient(patient);
        if (devolves.isEmpty()) {
            return false;
        }
        return devolves.stream()
                .anyMatch(devolve -> devolve.getDateReturnedToFacility() == null);
    }

    @Override
    public String getName() {
        return "Return Client to Facility";
    }

    @Override
    public String getPath() {
        return "devolves/return";
    }
}
