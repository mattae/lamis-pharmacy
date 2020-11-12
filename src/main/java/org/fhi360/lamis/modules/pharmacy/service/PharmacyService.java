package org.fhi360.lamis.modules.pharmacy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fhi360.lamis.modules.pharmacy.web.rest.vm.PharmacyDTO;
import org.fhi360.lamis.modules.pharmacy.web.rest.vm.RegimenInfo;
import org.lamisplus.modules.base.web.rest.JobSchedulerResource;
import org.lamisplus.modules.base.web.vm.ScheduleRequest;
import org.lamisplus.modules.lamis.legacy.domain.entities.*;
import org.lamisplus.modules.lamis.legacy.domain.entities.enumerations.ClientStatus;
import org.lamisplus.modules.lamis.legacy.domain.repositories.*;
import org.lamisplus.modules.lamis.legacy.service.PatientCurrentStatusService;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.sql.ResultSet;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static org.lamisplus.modules.lamis.legacy.domain.entities.enumerations.ClientStatus.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PharmacyService {
    private final PharmacyRepository pharmacyRepository;
    private final PatientRepository patientRepository;
    private final RegimenTypeRepository regimenTypeRepository;
    private final RegimenRepository regimenRepository;
    private final RegimenHistoryRepository regimenHistoryRepository;
    private final StatusHistoryRepository statusHistoryRepository;
    private final DevolveRepository devolveRepository;
    private final PatientCurrentStatusService patientCurrentStatusService;
    private final JobSchedulerResource jobSchedulerResource;
    private final JdbcTemplate jdbcTemplate;

    public Pharmacy savePharmacy(PharmacyDTO dto) {
        RegimenHistory regimenHistory = regimenHistoryRepository
                .getRegimenHistoryByPatientAt(dto.getPatient(), dto.getDateVisit()).orElse(null);
        Pharmacy pharmacy = new Pharmacy();
        pharmacy.setId(dto.getId());
        pharmacy.setPatient(dto.getPatient());
        pharmacy.setFacility(dto.getFacility());
        pharmacy.setMmdType(dto.getMmdType());
        pharmacy.setDateVisit(dto.getDateVisit());
        pharmacy.setNextAppointment(dto.getNextAppointment());
        pharmacy.setPrescriptionError(dto.getPrescriptionError());
        pharmacy.setAdherence(dto.getAdherence());
        pharmacy.setAdrScreened(dto.getAdrScreened());
        pharmacy.setExtra(dto.getExtra());
        pharmacy.setLines(dto.getLines());
        pharmacy.setUuid(dto.getUuid());

        dto.getLines().forEach(line -> {

            if (Arrays.asList(1L, 2L, 3L, 4L, 14L).contains(line.getRegimenTypeId())) {
                if (regimenHistory == null || !Objects.equals(regimenHistory.getRegimen().getId(), line.getRegimenId())) {
                    RegimenHistory history = new RegimenHistory();
                    history.setDateVisit(dto.getDateVisit());
                    Regimen regimen = new Regimen();
                    regimen.setId(line.getRegimenId());
                    history.setRegimen(regimen);
                    RegimenType type = new RegimenType();
                    type.setId(line.getRegimenTypeId());
                    history.setRegimenType(type);
                    history.setPatient(dto.getPatient());
                    history.setFacility(dto.getFacility());
                    regimenHistoryRepository.save(history);
                }
            }
        });
        ClientStatus status = getStatus(pharmacy.getPatient());
        List<StatusHistory> statusHistories1 = statusHistoryRepository
                .findByPatientAndDateStatusBefore(pharmacy.getPatient(), pharmacy.getDateVisit().plusDays(1)).stream()
                .sorted((s1, s2) -> s2.getDateStatus().compareTo(s1.getDateStatus()))
                .collect(Collectors.toList());
        if (statusHistories1.size() <= 1 && !status.equals(HIV_NEGATIVE)) {
            StatusHistory history = new StatusHistory();
            history.setFacility(pharmacy.getFacility());
            history.setPatient(pharmacy.getPatient());
            history.setDateStatus(pharmacy.getDateVisit());
            history.setStatus(ClientStatus.ART_START);
            Patient patient = patientRepository.findById(pharmacy.getPatient().getId()).orElse(null);
            if (patient != null && patient.getStatusAtRegistration().equals(ClientStatus.ART_TRANSFER_IN)) {
                history.setStatus(ClientStatus.ART_TRANSFER_IN);
            }
            statusHistoryRepository.save(history);
        } else {
            List<ClientStatus> statuses = Arrays.asList(STOPPED_TREATMENT, LOST_TO_FOLLOWUP, DID_NOT_ATTEMPT_TO_TRACE,
                    TRACED_AGREED_TO_RETURN_TO_CARE, TRACED_UNABLE_TO_LOCATE);
            if (statuses.contains(status) && !statusHistories1.get(0).getStatus().equals(ART_RESTART)) {
                StatusHistory history = new StatusHistory();
                history.setFacility(pharmacy.getFacility());
                history.setPatient(pharmacy.getPatient());
                history.setDateStatus(pharmacy.getDateVisit());
                history.setStatus(ART_RESTART);
                statusHistoryRepository.save(history);
            }
        }
        List<StatusHistory> statusHistories = statusHistoryRepository.findByPatientAndDateStatusBefore(pharmacy.getPatient(),
                pharmacy.getDateVisit().plusMonths(1));
        statusHistories.stream()
                .sorted((s1, s2) -> s2.getDateStatus().compareTo(s1.getDateStatus()))
                .forEach(s -> {
                    if (!s.getDateStatus().isBefore(pharmacy.getDateVisit()) && s.getAuto()) {
                        statusHistoryRepository.delete(s);
                    }
                });
        statusHistoryRepository.findByPatientAndDateStatusAfter(pharmacy.getPatient(),
                pharmacy.getDateVisit().minusDays(1), PageRequest.of(0, 100000)).forEach(history -> {
            if (history.getStatus() != null && history.getStatus().equals(LOST_TO_FOLLOWUP)) {
                statusHistoryRepository.delete(history);
            }
        });
        return pharmacyRepository.save(pharmacy);
    }

    public Pharmacy updatePharmacy(PharmacyDTO dto) {
        return savePharmacy(dto);
    }


    public RegimenInfo getRegimeInfo(Long patientId) {
        RegimenInfo regimenInfo = new RegimenInfo("", "");
        Patient patient = patientRepository.findById(patientId).orElse(null);
        if (patient != null) {
            Pharmacy pharmacy = pharmacyRepository.getLastRefillByPatient(patient);
            if (pharmacy != null) {
                PharmacyLine line = pharmacy.getLines().stream()
                        .filter(l -> (l.getRegimenTypeId() < 5) || l.getRegimenTypeId() == 14)
                        .findAny().get();

                regimenInfo = new RegimenInfo(
                        regimenTypeRepository.findById(line.getRegimenTypeId()).get().getDescription(),
                        regimenRepository.findById(line.getRegimenId()).get().getDescription());

            }
        }
        return regimenInfo;
    }

    public void deletePharmacy(String uuid) {
        pharmacyRepository.findByUuid(uuid).ifPresent(pharmacy -> deletePharmacy(pharmacy.getId()));
    }

    public void deletePharmacy(Long pharmacyId) {

        pharmacyRepository.findById(pharmacyId).ifPresent(pharmacy -> {
            Patient patient = pharmacy.getPatient();
            devolveRepository.findByPatient(patient)
                    .forEach(devolve -> {
                        if (devolve.getRelatedPharmacy() != null && Objects.equals(pharmacyId, devolve.getRelatedPharmacy().getId())) {
                            devolve.setRelatedPharmacy(null);
                            devolveRepository.save(devolve);
                        }
                    });

            statusHistoryRepository.getCurrentStatusForPatientAt(patient, pharmacy.getDateVisit())
                    .ifPresent(statusHistory -> {
                        if (statusHistory.getDateStatus().equals(pharmacy.getDateVisit())) {
                            statusHistoryRepository.delete(statusHistory);
                        }
                    });

            if (pharmacyRepository.findByPatientAndDateVisitAfter(patient, pharmacy.getDateVisit(),
                    PageRequest.of(0, Integer.MAX_VALUE)).isEmpty()) {
                statusHistoryRepository.findByPatientAndDateStatusAfter(patient, pharmacy.getDateVisit().minusDays(1),
                        PageRequest.of(0, Integer.MAX_VALUE))
                        .forEach(statusHistoryRepository::delete);
            }

            regimenHistoryRepository.getRegimenHistoryByPatientAt(patient, pharmacy.getDateVisit())
                    .ifPresent(history -> {
                        if (history.getDateVisit().equals(pharmacy.getDateVisit())) {
                            regimenHistoryRepository.delete(history);
                        }
                    });
            pharmacyRepository.delete(pharmacy);
        });
    }

    //@Transactional
    public void updateLostToFollowup() {
        patientRepository.findAll().stream()
                .filter(patient -> hasPharmacyVisitWithinLast6Months(patient.getId()))
                .forEach(patient -> {
                    pharmacyRepository.getLTFUDate(patient.getId()).ifPresent(date -> {
                        if (!convertToLocalDate(date).isBefore(LocalDate.now().minusMonths(6))
                                && !convertToLocalDate(date).isAfter(LocalDate.now())) {
                            Optional<StatusHistory> currentStatus = statusHistoryRepository.getCurrentStatusForPatient(patient);
                            if (!currentStatus.isPresent() ||
                                    !Arrays.asList(LOST_TO_FOLLOWUP, ART_TRANSFER_OUT, STOPPED_TREATMENT, KNOWN_DEATH)
                                            .contains(currentStatus.get().getStatus())) {
                                StatusHistory history = new StatusHistory();
                                history.setAuto(true);
                                history.setPatient(patient);
                                history.setFacility(patient.getFacility());
                                history.setDateStatus(convertToLocalDate(date));
                                history.setStatus(LOST_TO_FOLLOWUP);
                                statusHistoryRepository.save(history);
                            }
                        }
                    });
                });
    }

    public void updateJsonBPharmacyLine() {

    }

    @PostConstruct
    public void init() {

        String jobClass = "org.fhi360.lamis.modules.pharmacy.service.LostToFollowupJob";
        boolean scheduled = jobSchedulerResource.listJobClasses()
                .stream()
                .anyMatch(c -> c.equals(jobClass));
        if (!scheduled) {
            ScheduleRequest request = new ScheduleRequest();
            request.setJobClass(jobClass);
            request.setCronExpression("0 0 10 ? * *");
            try {
                jobSchedulerResource.scheduleJob(request);
            } catch (Exception ignored) {
            }
        }
    }

    private Boolean hasPharmacyVisitWithinLast6Months(Long patientId) {
        return jdbcTemplate.query("select date_visit from pharmacy where patient_id = ? and date_visit >= " +
                        "current_date - interval '6 months' and archived = false",
                ResultSet::next, patientId);
    }

    private LocalDate convertToLocalDate(Date date) {
        return Instant.ofEpochMilli(date.getTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }

    public ClientStatus getStatus(Patient patient) {
        return patientCurrentStatusService.getStatus(patient);
    }

    @Transactional
    public Long count() {
        return pharmacyRepository.count();
    }
}
