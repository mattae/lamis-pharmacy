package org.fhi360.lamis.modules.pharmacy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fhi360.lamis.modules.patient.service.providers.PatientSummaryProvider;
import org.fhi360.lamis.modules.patient.service.providers.vm.Field;
import org.fhi360.lamis.modules.patient.service.providers.vm.Summary;
import org.lamisplus.modules.lamis.legacy.domain.entities.Patient;
import org.lamisplus.modules.lamis.legacy.domain.entities.Pharmacy;
import org.lamisplus.modules.lamis.legacy.domain.entities.PharmacyLine;
//import org.lamisplus.modules.lamis.legacy.domain.repositories.PharmacyLineRepository;
import org.lamisplus.modules.lamis.legacy.domain.repositories.PharmacyRepository;
import org.lamisplus.modules.lamis.legacy.domain.repositories.RegimenRepository;
import org.lamisplus.modules.lamis.legacy.domain.repositories.RegimenTypeRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PharmacySummaryProvider implements PatientSummaryProvider {
    private final PharmacyRepository pharmacyRepository;
    private final RegimenTypeRepository regimenTypeRepository;
    private final RegimenRepository regimenRepository;
    //private final PharmacyLineRepository pharmacyLineRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<Summary> getSummaries(Patient patient) {
        Pharmacy pharmacy = pharmacyRepository.getLastRefillByPatient(patient);
        Optional<Pharmacy> iptRefill = pharmacyRepository.getIptRefills(patient)
                .stream().min((p1, p2) -> p2.getDateVisit().compareTo(p1.getDateVisit()));
        List<Summary> summaries = new ArrayList<>();
        Summary summary = new Summary();
        summary.setHeader("Refill Summary");
        List<Field> fields = new ArrayList<>();
        if (pharmacy != null) {
            /*List<PharmacyLine> lines = pharmacyLineRepository.findByPharmacy(pharmacy);*/
            Set<PharmacyLine> lines = pharmacy.getLines();
            PharmacyLine line = lines.stream()
                    .filter(l -> Arrays.asList(1L, 2L, 3L, 4L, 14L).contains(l.getRegimenTypeId()))
                    .findFirst().get();
            Field field = new Field(Field.FieldType.TEXT, "Current Regimen", regimenRepository.findById(line.getRegimenId()).get().getDescription());
            fields.add(field);
            field = new Field(Field.FieldType.TEXT, "Current Regimen Line", regimenTypeRepository.findById(line.getRegimenTypeId()).get().getDescription());
            fields.add(field);
            field = new Field(Field.FieldType.DATE, "Refill Date", pharmacy.getDateVisit());
            fields.add(field);
            field = new Field(Field.FieldType.INT, "Refill Duration", line.getDuration());
            fields.add(field);
            summary.setFields(fields);
            summaries.add(summary);

            /*iptRefill.ifPresent(p -> pharmacyLineRepository.findByPharmacy(p).stream()*/
            iptRefill.ifPresent(p -> p.getLines().stream()
                    .filter(l -> Objects.equals(l.getRegimenTypeId(), 15L))
                    .findFirst().ifPresent(pl -> {
                        fields.add(new Field(Field.FieldType.TEXT, "IPT", regimenTypeRepository.findById(pl.getRegimenTypeId()).get().getDescription()));
                        fields.add(new Field(Field.FieldType.DATE, "IPT Date", p.getDateVisit()));
                    }));
        } else {
            Field field = new Field(Field.FieldType.TEXT, "Current Regimen", null);
            fields.add(field);
            field = new Field(Field.FieldType.TEXT, "Current Regimen Line", null);
            field = new Field(Field.FieldType.DATE, "Refill Date", null);
            fields.add(field);
            field = new Field(Field.FieldType.INT, "Refill Duration", null);
            fields.add(field);
            /*iptRefill.ifPresent(p -> pharmacyLineRepository.findByPharmacy(p).stream()*/
            iptRefill.ifPresent(p -> p.getLines().stream()
                    .filter(l -> Objects.equals(l.getRegimenTypeId(), 15L))
                    .findFirst().ifPresent(pl -> {
                        fields.add(new Field(Field.FieldType.TEXT, "IPT", regimenTypeRepository.findById(pl.getRegimenTypeId()).get().getDescription()));
                        fields.add(new Field(Field.FieldType.DATE, "IPT Date", p.getDateVisit()));
                    }));
            summary.setFields(fields);
            summaries.add(summary);
        }
        return summaries;
    }
}
