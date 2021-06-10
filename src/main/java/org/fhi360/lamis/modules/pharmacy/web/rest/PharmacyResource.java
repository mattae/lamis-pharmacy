package org.fhi360.lamis.modules.pharmacy.web.rest;

import io.github.jhipster.web.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fhi360.lamis.modules.pharmacy.service.PharmacyService;
import org.fhi360.lamis.modules.pharmacy.web.rest.vm.DrugDTO;
import org.fhi360.lamis.modules.pharmacy.web.rest.vm.PharmacyDTO;
import org.fhi360.lamis.modules.pharmacy.web.rest.vm.RegimenInfo;
import org.lamisplus.modules.base.web.errors.BadRequestAlertException;
import org.lamisplus.modules.base.web.util.HeaderUtil;
import org.lamisplus.modules.lamis.legacy.domain.entities.*;
import org.lamisplus.modules.lamis.legacy.domain.entities.enumerations.ClientStatus;
import org.lamisplus.modules.lamis.legacy.domain.repositories.*;
import org.lamisplus.modules.lamis.legacy.domain.repositories.projections.VisitDates;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class PharmacyResource {
    private static final String ENTITY_NAME = "pharmacy";

    private final PharmacyRepository pharmacyRepository;
    private final PatientRepository patientRepository;
    private final StatusHistoryRepository statusHistoryRepository;
    private final AdverseDrugReactionRepository adverseDrugReactionRepository;
    private final RegimenRepository regimenRepository;
    private final RegimenTypeRepository regimenTypeRepository;
    private final RegimenDrugRepository regimenDrugRepository;
    private final DevolveRepository devolveRepository;
    private final PharmacyService pharmacyService;

    /**
     * POST  /pharmacies : Create a new pharmacy.
     *
     * @param pharmacy the pharmacy to create
     * @return the ResponseEntity with status 201 (Created) and with body the new pharmacy, or with status 400 (Bad Request) if the pharmacy has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/pharmacies")
    public ResponseEntity<Pharmacy> createPharmacy(@RequestBody PharmacyDTO pharmacy) throws URISyntaxException {
        LOG.debug("REST request to save Pharmacy");
        if (pharmacy.getId() != null) {
            throw new BadRequestAlertException("A new pharmacy cannot already have an ID", ENTITY_NAME, "idexists");
        }

        Pharmacy result = pharmacyService.savePharmacy(pharmacy);
        return ResponseEntity.created(new URI("/api/pharmacy/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /pharmacies : Updates an existing pharmacy.
     *
     * @param pharmacy the pharmacy to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated pharmacy,
     * or with status 400 (Bad Request) if the pharmacy is not valid,
     * or with status 500 (Internal Server Error) if the pharmacy couldn't be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/pharmacies")
    public ResponseEntity<Pharmacy> updatePharmacy(@RequestBody PharmacyDTO pharmacy) throws URISyntaxException {
        LOG.debug("REST request to update pharmacy : {}", pharmacy);
        if (pharmacy.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }

        Pharmacy result = pharmacyService.updatePharmacy(pharmacy);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, pharmacy.getId().toString()))
            .body(result);
    }

    /**
     * GET  /pharmacies/:id : get the "id" pharmacy.
     *
     * @param id the id of the pharmacy to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the pharmacy, or with status 404 (Not Found)
     */
    @GetMapping("/pharmacies/{id}")
    public ResponseEntity<Pharmacy> getPharmacy(@PathVariable Long id) {
        LOG.debug("REST request to get pharmacy : {}", id);
        Optional<Pharmacy> pharmacy = pharmacyRepository.findById(id);
        return ResponseUtil.wrapOrNotFound(pharmacy);
    }

    /**
     * GET  /pharmacies/:id : get the "uuid" pharmacy.
     *
     * @param id the uuid of the pharmacy to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the pharmacy, or with status 404 (Not Found)
     */
    @GetMapping("/pharmacies/by-uuid/{id}")
    public ResponseEntity<Pharmacy> getPharmacyByUuid(@PathVariable String id) {
        LOG.debug("REST request to get pharmacy : {}", id);
        Optional<Pharmacy> pharmacy = pharmacyRepository.findByUuid(id);
        return ResponseUtil.wrapOrNotFound(pharmacy);
    }

    @GetMapping("/pharmacies/patient/{id}/latest")
    public ResponseEntity<Pharmacy> getLatestPharmacyVisit(@PathVariable Long id) {
        LOG.debug("REST request to get latest pharmacy : {}", id);
        Patient patient = patientRepository.getOne(id);
        Pharmacy pharmacy = pharmacyRepository.getLastRefillByPatient(patient);
        return ResponseUtil.wrapOrNotFound(Optional.of(pharmacy));
    }

    /**
     * DELETE  /pharmacies/:id : delete the "id" pharmacy.
     *
     * @param id the id of the pharmacy to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/pharmacies/{id}")
    public ResponseEntity<Void> deletePharmacy(@PathVariable Long id) {
        LOG.debug("REST request to delete pharmacy : {}", id);

        pharmacyService.deletePharmacy(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build();
    }

    /**
     * DELETE  /pharmacies/:id : delete the "uuid" pharmacy.
     *
     * @param id the uuid of the pharmacy to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/pharmacies/by-uuid/{id}")
    public ResponseEntity<Void> deletePharmacyByUuid(@PathVariable String id) {
        LOG.debug("REST request to delete pharmacy : {}", id);

        pharmacyService.deletePharmacy(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id)).build();
    }

    @GetMapping("/pharmacies/adverse-drug-reactions")
    public List<AdverseDrugReaction> getAdverseDrugReactions() {
        return adverseDrugReactionRepository.findAll();
    }

    @GetMapping("/pharmacies/regimen-types")
    public List<RegimenType> getRegimenTypes() {
        return regimenTypeRepository.findAll();
    }

    /*@GetMapping("/pharmacies/{pharmacyId}/lines")
    public List<PharmacyLineDTO> getPharmacyLines(@PathVariable Long pharmacyId) {
        return pharmacyService.getPharmacyLines(pharmacyId);
    }*/

    @GetMapping("/pharmacies/regimen-info/patient/{patientId}")
    public RegimenInfo getRegimenInfo(@PathVariable Long patientId) {
        return pharmacyService.getRegimeInfo(patientId);
    }

    @GetMapping("/pharmacies/drugs/regimen/{regimenId}")
    public List<DrugDTO> getDrugs(@PathVariable Long regimenId) {
        Optional<Regimen> regimen = regimenRepository.findById(regimenId);
        return regimen.map(value -> regimenDrugRepository.findByRegimen(value)
            .stream()
            .map(DrugDTO::new)
            .collect(toList())).orElseGet(ArrayList::new);
    }

    @GetMapping("/pharmacies/regimens/regimen-type/{regimenTypeId}")
    public List<Regimen> getRegimensByRegimenType(@PathVariable Long regimenTypeId) {
        Optional<RegimenType> regimenType = regimenTypeRepository.findById(regimenTypeId);
        if (regimenType.isPresent()) {
            return regimenRepository.findByRegimenTypeAndActiveTrueOrderByPriorityDesc(regimenType.get());
        }
        return new ArrayList<>();
    }

    @GetMapping("/pharmacies/regimen/{id}")
    public ResponseEntity<Regimen> getRegimenById(@PathVariable Long id) {
        return ResponseUtil.wrapOrNotFound(regimenRepository.findById(id));
    }

    @GetMapping("/pharmacies/patient/{id}/visit-dates")
    public List<LocalDate> getVisitDatesByPatient(@PathVariable Long id) {
        List<LocalDate> visitDates = new ArrayList<>();
        patientRepository.findById(id).ifPresent(patient -> {
            List<LocalDate> dates = pharmacyRepository.findVisitsByPatient(patient).stream()
                .map(VisitDates::getDateVisit)
                .collect(toList());
            visitDates.addAll(dates);
        });
        return visitDates;
    }

    @GetMapping("/pharmacies/patient/{id}/devolvement/at/{date}")
    public ResponseEntity<Devolve> getCurrentDevolvementByPatient(@PathVariable Long id, @PathVariable LocalDate date) {
        Optional<Devolve> devolve = patientRepository.findById(id).flatMap(patient -> {
            List<Devolve> devolves = devolveRepository.findByPatientAndDateDevolvedBefore(patient, date.plusDays(1),
                PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "dateDevolved")));
            return !devolves.isEmpty() ? Optional.of(devolves.get(0)) : Optional.empty();
        });

        return ResponseUtil.wrapOrNotFound(devolve);
    }

    @GetMapping("/pharmacies/patient/{id}/has-dead-status")
    public ResponseEntity<StatusHistory> hasDeadStatus(@PathVariable String id) {
        List<StatusHistory> histories = patientRepository.findByUuid(id).flatMap(patient ->
            Optional.of(statusHistoryRepository.findByPatient(patient).stream()
                .filter(statusHistory -> statusHistory.getStatus() != null &&
                    statusHistory.getStatus().equals(ClientStatus.KNOWN_DEATH)).collect(toList())))
            .orElse(null);
        if (histories != null && !histories.isEmpty()) {
            return ResponseEntity.of(histories.stream().min((h1, h2) -> h2.getDateStatus().compareTo(h1.getDateStatus())));
        }

        return ResponseEntity.of(Optional.empty());
    }

    @GetMapping("/pharmacies/patient/{patientId}/last-ipt-at/{date}")
    public LocalDate dateOfLastIptBefore(@PathVariable Long patientId, @PathVariable LocalDate date) {
        return pharmacyService.dateOfLastIptBefore(patientId, date);
    }

    @GetMapping("/pharmacies/patient/{patientId}/uncompleted-ipt-after/{date}")
    public Boolean hasUncompletedIptAfter(@PathVariable Long patientId, @PathVariable LocalDate date) {
        return pharmacyService.hasUncompletedIptAfter(patientId, date);
    }

    @GetMapping("/pharmacies/patient/{patientId}/complete-tpt/{date}")
    public void completeTpt(@PathVariable Long patientId, @PathVariable LocalDate date) {
        pharmacyService.completeTpt(patientId, date);
    }
}
