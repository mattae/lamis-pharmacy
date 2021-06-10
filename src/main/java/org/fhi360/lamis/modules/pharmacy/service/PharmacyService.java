package org.fhi360.lamis.modules.pharmacy.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fhi360.lamis.modules.pharmacy.web.rest.vm.PharmacyDTO;
import org.fhi360.lamis.modules.pharmacy.web.rest.vm.RegimenInfo;
import org.lamisplus.modules.lamis.legacy.domain.entities.*;
import org.lamisplus.modules.lamis.legacy.domain.entities.enumerations.ClientStatus;
import org.lamisplus.modules.lamis.legacy.domain.repositories.*;
import org.lamisplus.modules.lamis.legacy.service.PatientCurrentStatusService;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.sql.ResultSet;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
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
    private final JdbcTemplate jdbcTemplate;
    private final static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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

        assert pharmacy.getPatient().getId() != null;

        AtomicBoolean artRefill = new AtomicBoolean(false);

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
                    artRefill.set(true);
                }
            }
        });
        ClientStatus status = getStatus(pharmacy.getPatient());
        List<StatusHistory> statusHistories1 = statusHistoryRepository
            .findByPatientAndDateStatusBefore(pharmacy.getPatient(), pharmacy.getDateVisit().plusDays(1)).stream()
            .sorted((s1, s2) -> s2.getDateStatus().compareTo(s1.getDateStatus()))
            .collect(Collectors.toList());
        StatusHistory history = new StatusHistory();
        history.setFacility(pharmacy.getFacility());
        history.setPatient(pharmacy.getPatient());
        history.setDateStatus(pharmacy.getDateVisit());
        if (statusHistories1.size() <= 1 && !status.equals(HIV_NEGATIVE)) {
            history.setStatus(ClientStatus.ART_START);
            Patient patient = patientRepository.findById(pharmacy.getPatient().getId()).orElse(null);
            if (patient != null && patient.getStatusAtRegistration().equals(ClientStatus.ART_TRANSFER_IN)) {
                history.setStatus(ClientStatus.ART_TRANSFER_IN);
                statusHistoryRepository.save(history);
            }
        }
        List<ClientStatus> statuses = Arrays.asList(STOPPED_TREATMENT, LOST_TO_FOLLOWUP, DID_NOT_ATTEMPT_TO_TRACE,
            TRACED_AGREED_TO_RETURN_TO_CARE, TRACED_UNABLE_TO_LOCATE);
        if (statuses.contains(status) && !(!statusHistories1.isEmpty() && (statusHistories1.get(0).getStatus().equals(ART_RESTART)) ||
            statusHistories1.get(0).getStatus().equals(RETURN_TO_CARE))) {
            Optional<Date> ltfu = this.pharmacyRepository.getLTFUDate(pharmacy.getPatient().getId());
            if (ltfu.isPresent()) {
                int ltfuQuarter = (convertToLocalDate(ltfu.get()).getMonthValue() / 3) + 1;
                int visitQuarter = (pharmacy.getDateVisit().getMonthValue() / 3) + 1;
                int ltfuYear = convertToLocalDate(ltfu.get()).getYear();
                int visitYear = pharmacy.getDateVisit().getYear();
                if (ltfuQuarter == visitQuarter && ltfuYear == visitYear) {
                    history.setStatus(RETURN_TO_CARE);
                } else {
                    history.setStatus(ART_RESTART);
                }
            } else {
                history.setStatus(ART_RESTART);
            }
            statusHistoryRepository.save(history);
        }
        if (!statusHistories1.isEmpty() && (statusHistories1.get(0).getStatus().equals(ART_RESTART) ||
            statusHistories1.get(0).getStatus().equals(RETURN_TO_CARE))) {
            history.setStatus(ART_START);
            statusHistoryRepository.save(history);
        }

        if (!statusHistories1.isEmpty() && statusHistories1.get(0).getStatus().equals(ART_TRANSFER_OUT)) {
            history.setStatus(ART_TRANSFER_IN);
            statusHistoryRepository.save(history);
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
            pharmacy.getDateVisit().minusDays(1), PageRequest.of(0, 100000)).forEach(hist -> {
            if (hist.getStatus() != null && hist.getStatus().equals(LOST_TO_FOLLOWUP)) {
                statusHistoryRepository.delete(hist);
            }
        });
        patientRepository.findById(pharmacy.getPatient().getId()).ifPresent(patient -> {
            if (patient.getDateStarted() == null && artRefill.get()) {
                patient.setDateStarted(pharmacy.getDateVisit());
                patientRepository.save(patient);
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

            statusHistoryRepository.findByPatientAndDateStatus(patient, pharmacy.getDateVisit())
                .forEach(statusHistory -> {
                    if (statusHistory.getDateStatus().equals(pharmacy.getDateVisit())) {
                        if (pharmacyRepository.findByPatientAndDateVisit(patient, pharmacy.getDateVisit()).size() <= 1) {
                            statusHistoryRepository.delete(statusHistory);
                        }
                    }
                });

            regimenHistoryRepository.getRegimenHistoryByPatientAt(patient, pharmacy.getDateVisit().plusDays(1))
                .ifPresent(history -> {
                    if (history.getDateVisit().equals(pharmacy.getDateVisit())) {
                        if (pharmacyRepository.findByPatientAndDateVisit(patient, pharmacy.getDateVisit()).size() <= 1) {
                            regimenHistoryRepository.delete(history);
                        }
                    }
                });
            pharmacyRepository.delete(pharmacy);
        });
    }

    public void completeTpt(Long patientId, LocalDate date) {
        patientRepository.findById(patientId).ifPresent(patient -> {
            LocalDate lastTpt = dateOfLastIptBefore(patient.getId(), LocalDate.now());
            pharmacyRepository.findByPatientAndDateVisitAfter(patient, lastTpt.minusDays(1),
                PageRequest.of(0, Integer.MAX_VALUE)).stream()
                .filter(pharmacy -> !pharmacy.getDateVisit().isAfter(LocalDate.now()))
                .sorted((p1, p2) -> p2.getDateVisit().compareTo(p1.getDateVisit()))
                .limit(1)
                .forEach(pharmacy -> {
                    JsonNode extra = pharmacy.getExtra();
                    if (extra == null) {
                        extra = OBJECT_MAPPER.createObjectNode();
                    }
                    JsonNode tpt = extra.get("ipt");
                    if (tpt == null) {
                        tpt = OBJECT_MAPPER.createObjectNode();
                    }
                    tpt = ((ObjectNode) tpt.deepCopy()).put("dateCompleted", date.toString());
                    extra = ((ObjectNode) extra.deepCopy()).set("ipt", tpt);
                    pharmacy.setExtra(extra);
                    pharmacyRepository.save(pharmacy);
                });
        });
    }

    @Scheduled(cron = "0 0/30 * * * ?")
    public void updateLostToFollowup() {
        fixFutureTransferOut();
        jdbcTemplate.execute("select fix_status()");
        jdbcTemplate.execute("select fix_iit()");
        jdbcTemplate.execute("select fix_iit2()");
        patientRepository.findAll()
            .forEach(patient -> {
                pharmacyRepository.getLTFUDate(patient.getId()).ifPresent(date -> {
                    if (!convertToLocalDate(date).isAfter(LocalDate.now())) {
                        ClientStatus currentStatus = getStatus(patient, convertToLocalDate(date));
                        if (currentStatus != null &&
                            !Arrays.asList(LOST_TO_FOLLOWUP, ART_TRANSFER_OUT, STOPPED_TREATMENT, KNOWN_DEATH, HIV_EXPOSED_STATUS_UNKNOWN,
                                PRE_ART_TRANSFER_OUT, PRE_ART_TRANSFER_IN, HIV_NEGATIVE, PREVIOUSLY_UNDOCUMENTED_TRANSFER_CONFIRMED)
                                .contains(currentStatus)) {
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
        jdbcTemplate.execute("select fix_status()");
        jdbcTemplate.execute("select fix_iit()");
        jdbcTemplate.execute("select fix_iit2()");
    }

    private void fixFutureTransferOut() {
        jdbcTemplate.queryForList("" +
            "select * from status_history where archived = true and (extra->>'futureTransferOut')::bool = true")
            .forEach(sh -> {
                Long patientId = (Long) sh.get("patient_id");
                pharmacyRepository.getLTFUDate(patientId).ifPresent(date -> {
                    if (!convertToLocalDate(date).minusDays(29).isAfter(LocalDate.now())) {
                        Long id = (Long) sh.get("id");
                        jdbcTemplate.update("update status_history set archived = false where id = ?", id);
                        statusHistoryRepository.findById(id).ifPresent(statusHistory -> {
                            statusHistory.setDateStatus(convertToLocalDate(date));
                            statusHistory.setArchived(false);
                            JsonNode extra = statusHistory.getExtra();
                            statusHistory.setExtra(((ObjectNode) extra).without("futureTransferOut"));
                            statusHistoryRepository.save(statusHistory);
                        });
                    }
                });
            });
    }

    private LocalDate convertToLocalDate(Date date) {
        return Instant.ofEpochMilli(date.getTime())
            .atZone(ZoneId.systemDefault())
            .toLocalDate();
    }

    public ClientStatus getStatus(Patient patient) {
        return patientCurrentStatusService.getStatus(patient);
    }

    public LocalDate dateOfLastIptBefore(Long patientId, LocalDate date) {
        try {
            return jdbcTemplate.queryForObject("select date_visit from pharmacy p, jsonb_array_elements(lines) with ordinality a(l) where " +
                "cast(jsonb_extract_path_text(l,'regimen_type_id') as integer) = 15 and patient_id = ? and date_visit <= ? " +
                "and archived = false and extra->'ipt'->>'type' like '%_INITIATION' order by date_visit desc limit 1", LocalDate.class, patientId, date);
        } catch (Exception e) {
            return null;
        }
    }

    public Boolean hasUncompletedIptAfter(Long patientId, LocalDate date) {
        return !jdbcTemplate.query("select id from pharmacy where extra->'ipt'->>'dateCompleted' is not null and " +
            "patient_id = ? and date_visit > ?", ResultSet::next, patientId, date);
    }

    public ClientStatus getStatus(Patient patient, LocalDate atDate) {
        ClientStatus status = patient.getStatusAtRegistration();
        Optional<Date> date = this.pharmacyRepository.getLTFUDate(patient.getId());
        Optional<StatusHistory> statusHistory = this.statusHistoryRepository.getCurrentStatusForPatientAt(patient, atDate.plusDays(1L));
        if (!date.isPresent()) {
            if (patient.getStatusAtRegistration() != null) {
                status = patient.getStatusAtRegistration();
            } else if (statusHistory.isPresent()) {
                status = statusHistory.get().getStatus();
            }
        } else {
            LocalDate ltfuDate = Instant.ofEpochMilli(date.get().getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
            if (!ltfuDate.isBefore(LocalDate.now())) {
                if (statusHistory.isPresent() && statusHistory.get().getStatus().equals(ClientStatus.KNOWN_DEATH)) {
                    status = ClientStatus.KNOWN_DEATH;
                } else if (statusHistory.isPresent() && statusHistory.get().getStatus().equals(ClientStatus.ART_TRANSFER_OUT)) {
                    status = ClientStatus.ART_TRANSFER_OUT;
                } else if (statusHistory.isPresent() && statusHistory.get().getStatus().equals(ClientStatus.STOPPED_TREATMENT)) {
                    status = ClientStatus.STOPPED_TREATMENT;
                } else if (statusHistory.isPresent()) {
                    StatusHistory history = statusHistory.get();
                    if (history.getStatus().equals(ClientStatus.ART_RESTART)) {
                        status = ClientStatus.ART_RESTART;
                    } else if (history.getStatus().equals(ClientStatus.ART_START)) {
                        status = ClientStatus.ART_START;
                    } else if (history.getStatus().equals(ClientStatus.ART_TRANSFER_IN)) {
                        status = ClientStatus.ART_TRANSFER_IN;
                    } else {
                        status = ClientStatus.ART_START;
                    }
                } else {
                    status = ClientStatus.ART_START;
                }
            } else if (statusHistory.isPresent() && statusHistory.get().getStatus().equals(ClientStatus.KNOWN_DEATH)) {
                status = ClientStatus.KNOWN_DEATH;
            } else if (statusHistory.isPresent() && statusHistory.get().getStatus().equals(ClientStatus.ART_TRANSFER_OUT)) {
                status = ClientStatus.ART_TRANSFER_OUT;
            } else if (statusHistory.isPresent() && statusHistory.get().getStatus().equals(ClientStatus.STOPPED_TREATMENT)) {
                status = ClientStatus.STOPPED_TREATMENT;
            } else {
                status = ClientStatus.LOST_TO_FOLLOWUP;
            }
        }

        return status;
    }

    @PostConstruct
    public void init() {
        jdbcTemplate.execute("update status_history set status = 'ART_RESTART', last_modified = current_timestamp where status = '' or status is null and date_status > '2021-05-01'");
    }
}
