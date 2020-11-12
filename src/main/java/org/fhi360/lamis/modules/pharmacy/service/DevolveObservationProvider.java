package org.fhi360.lamis.modules.pharmacy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fhi360.lamis.modules.patient.service.providers.PatientObservationViewProvider;
import org.lamisplus.modules.lamis.legacy.domain.entities.Devolve;
import org.lamisplus.modules.lamis.legacy.domain.entities.Patient;
import org.lamisplus.modules.lamis.legacy.domain.entities.enumerations.DmocType;
import org.lamisplus.modules.lamis.legacy.domain.repositories.DevolveRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DevolveObservationProvider implements PatientObservationViewProvider {
    private final DevolveRepository devolveRepository;

    @Override
    public boolean applicableTo(Patient patient) {
        List<Devolve> devolves = devolveRepository.findByPatient(patient);
        if (devolves.isEmpty()) {
            return true;
        }
        return devolves.stream()
                .filter(devolve -> !(devolve.getDmocType().equals(DmocType.MMD) || devolve.getDmocType().equals(DmocType.MMS)))
                .noneMatch(devolve -> devolve.getDateReturnedToFacility() == null);
    }

    @Override
    public String getName() {
        return "Devolve Patient";
    }

    @Override
    public String getPath() {
        return "devolves";
    }
}
