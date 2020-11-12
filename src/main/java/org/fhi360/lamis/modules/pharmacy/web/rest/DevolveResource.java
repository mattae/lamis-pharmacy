package org.fhi360.lamis.modules.pharmacy.web.rest;

import io.github.jhipster.web.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.fhi360.lamis.modules.pharmacy.web.rest.vm.RelatedCD4;
import org.fhi360.lamis.modules.pharmacy.web.rest.vm.RelatedClinic;
import org.fhi360.lamis.modules.pharmacy.web.rest.vm.RelatedPharmacy;
import org.fhi360.lamis.modules.pharmacy.web.rest.vm.RelatedViralLoad;
import org.lamisplus.modules.base.domain.repositories.ProvinceRepository;
import org.lamisplus.modules.base.web.errors.BadRequestAlertException;
import org.lamisplus.modules.base.web.util.HeaderUtil;
import org.lamisplus.modules.lamis.legacy.domain.entities.*;
import org.lamisplus.modules.lamis.legacy.domain.repositories.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class DevolveResource {
    private final static String ENTITY_NAME = "devolve";
    private final DevolveRepository devolveRepository;
    private final PatientRepository patientRepository;
    private final LaboratoryRepository laboratoryRepository;
    private final RegimenRepository regimenRepository;
    //private final LaboratoryLineRepository laboratoryLineRepository;
    private final PharmacyRepository pharmacyRepository;
    //private final PharmacyLineRepository pharmacyLineRepository;
    private final ClinicRepository clinicRepository;
    private final ProvinceRepository provinceRepository;
    private final CommunityPharmacyRepository communityPharmacyRepository;

    /**
     * POST  /devolves : Create a new devolve.
     *
     * @param devolve the devolve to create
     * @return the ResponseEntity with status 201 (Created) and with body the new devolve, or with status 400 (Bad Request) if the devolve has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/devolves")
    public ResponseEntity<Devolve> createDevolve(@RequestBody Devolve devolve) throws URISyntaxException {
        LOG.debug("REST request to save Devolve");
        if (devolve.getId() != null) {
            throw new BadRequestAlertException("A new devolve cannot already have an ID", ENTITY_NAME, "idexists");
        }

        Devolve result = devolveRepository.save(devolve);
        return ResponseEntity.created(new URI("/api/devolves/" + result.getId()))
                .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.getId().toString()))
                .body(result);
    }

    /**
     * PUT  /devolves : Updates an existing devolve.
     *
     * @param devolve the devolve to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated devolve,
     * or with status 400 (Bad Request) if the devolve is not valid,
     * or with status 500 (Internal Server Error) if the devolve couldn't be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/devolves")
    public ResponseEntity<Devolve> updateDevolve(@RequestBody Devolve devolve) throws URISyntaxException {
        LOG.debug("REST request to update devolve : {}", devolve);
        if (devolve.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }

        Devolve result = devolveRepository.save(devolve);
        return ResponseEntity.ok()
                .headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, devolve.getId().toString()))
                .body(result);
    }

    /**
     * GET  /devolves/:id : get the "id" devolve.
     *
     * @param id the id of the devolve to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the devolve, or with status 404 (Not Found)
     */
    @GetMapping("/devolves/{id}")
    public ResponseEntity<Devolve> getDevolve(@PathVariable Long id) {
        LOG.debug("REST request to get devolve : {}", id);
        Optional<Devolve> devolve = devolveRepository.findById(id);
        return ResponseUtil.wrapOrNotFound(devolve);
    }

    /**
     * GET  /devolves/:id : get the "uuid" devolve.
     *
     * @param id the uuid of the devolve to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the devolve, or with status 404 (Not Found)
     */
    @GetMapping("/devolves/by-uuid/{id}")
    public ResponseEntity<Devolve> getDevolveByUuid(@PathVariable String id) {
        LOG.debug("REST request to get devolve : {}", id);
        Optional<Devolve> devolve = devolveRepository.findByUuid(id);
        return ResponseUtil.wrapOrNotFound(devolve);
    }

    /**
     * DELETE  /devolves/:id : delete the "id" devolve.
     *
     * @param id the id of the devolve to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/devolves/{id}")
    public ResponseEntity<Void> deleteDevolve(@PathVariable Long id) {
        LOG.debug("REST request to delete devolve : {}", id);

        devolveRepository.deleteById(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build();
    }

    /**
     * DELETE  /devolves/:id : delete the "uuid" devolve.
     *
     * @param id the uuid of the devolve to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/devolves/by-uuid/{id}")
    public ResponseEntity<Void> deleteDevolveByUuid(@PathVariable String id) {
        LOG.debug("REST request to delete devolve : {}", id);

        devolveRepository.findByUuid(id).ifPresent(devolveRepository::delete);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id)).build();
    }

    @GetMapping("/devolves/community-pharmacies/lga/{id}")
    public List<CommunityPharmacy> getCommunityPharmaciesByLga(@PathVariable Long id) {
        return provinceRepository.findById(id).map(communityPharmacyRepository::findByLga).orElse(new ArrayList<>())
                .stream()
                .filter(CommunityPharmacy::getActive)
                .collect(Collectors.toList());
    }

    @GetMapping("/devolves/{devolveId}/patient/{id}/related-pharmacy/at/{date}")
    public ResponseEntity<RelatedPharmacy> getRelatedPharmacyByPatientAt(@PathVariable Long devolveId, @PathVariable Long id,
                                                                         @PathVariable LocalDate date) {
        Optional<RelatedPharmacy> related = devolveRepository.findById(devolveId).map(d -> {
            Pharmacy pharmacy = d.getRelatedPharmacy();
            final RelatedPharmacy[] relatedPharmacy = {null};
            if (pharmacy != null) {
                Set<PharmacyLine> lines = pharmacy.getLines();
                for (PharmacyLine line : lines) {
                    if (Arrays.asList(1L, 2L, 3L, 4L, 14L).contains(line.getRegimenTypeId())) {
                        relatedPharmacy[0] = new RelatedPharmacy();
                        relatedPharmacy[0].setId(pharmacy.getId());
                        relatedPharmacy[0].setDateVisit(pharmacy.getDateVisit());
                        regimenRepository.findById(line.getRegimenId()).ifPresent(regimen ->
                                relatedPharmacy[0].setRegimen(regimen.getDescription()));
                    }
                }
            }
            return Optional.ofNullable(relatedPharmacy[0]);
        }).orElse(patientRepository.findById(id).flatMap(patient -> {
            List<Pharmacy> pharmacies = pharmacyRepository.findByPatientAndDateVisitBefore(patient, date,
                    PageRequest.of(0, 3, Sort.by("dateVisit").descending()));
            for (Pharmacy pharmacy : pharmacies) {
                Set<PharmacyLine> lines = pharmacy.getLines();
                for (PharmacyLine line : lines) {
                    if (Arrays.asList(1L, 2L, 3L, 4L, 14L).contains(line.getRegimenTypeId())) {
                        RelatedPharmacy relatedPharmacy = new RelatedPharmacy();
                        relatedPharmacy.setId(pharmacy.getId());
                        relatedPharmacy.setDateVisit(pharmacy.getDateVisit());
                        regimenRepository.findById(line.getRegimenId()).ifPresent(regimen ->
                                relatedPharmacy.setRegimen(regimen.getDescription()));
                        return Optional.of(relatedPharmacy);
                    }
                }
            }
            return Optional.empty();
        }));
        return ResponseUtil.wrapOrNotFound(related);
    }

    @GetMapping("/devolves/{devolveId}/patient/{id}/related-viral-load/at/{date}")
    public ResponseEntity<RelatedViralLoad> getRelatedViralLoadByPatientAt(@PathVariable Long devolveId, @PathVariable Long id,
                                                                           @PathVariable LocalDate date) {
        Optional<RelatedViralLoad> related = devolveRepository.findById(devolveId).map(d -> {
            Laboratory laboratory = d.getRelatedCd4();
            RelatedViralLoad relatedViralLoad = null;
            if (laboratory != null) {
                /*List<LaboratoryLine> lines = laboratoryLineRepository.findByLaboratory(laboratory);*/
                Set<LaboratoryLine> lines = laboratory.getLines();
                for (LaboratoryLine line : lines) {
                    if (Long.valueOf(16L).equals(line.getLabTestId())) {
                        relatedViralLoad = new RelatedViralLoad();
                        relatedViralLoad.setId(laboratory.getId());
                        relatedViralLoad.setDateResultReceived(laboratory.getDateResultReceived());
                        try {
                            relatedViralLoad.setValue(Double.parseDouble(line.getResult()));
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
            return Optional.ofNullable(relatedViralLoad);
        }).orElse(patientRepository.findById(id).flatMap(patient -> {
            List<Laboratory> laboratories = laboratoryRepository.findByPatientAndDateResultReceivedBefore(patient, date,
                    PageRequest.of(0, 3, Sort.by("dateResultReceived").descending()));
            for (Laboratory laboratory : laboratories) {
                /*List<LaboratoryLine> lines = laboratoryLineRepository.findByLaboratory(laboratory);*/
                Set<LaboratoryLine> lines = laboratory.getLines();
                for (LaboratoryLine line : lines) {
                    if (Long.valueOf(16L).equals(line.getLabTestId())) {
                        RelatedViralLoad relatedViralLoad = new RelatedViralLoad();
                        relatedViralLoad.setId(laboratory.getId());
                        relatedViralLoad.setDateResultReceived(laboratory.getDateResultReceived());
                        try {
                            relatedViralLoad.setValue(Double.parseDouble(line.getResult()));
                        } catch (Exception ignored) {
                        }
                        return Optional.of(relatedViralLoad);
                    }
                }
            }
            return Optional.empty();
        }));
        return ResponseUtil.wrapOrNotFound(related);
    }

    @GetMapping("/devolves/{devolveId}/patient/{id}/related-cd4/at/{date}")
    public ResponseEntity<RelatedCD4> getRelatedCD4ByPatientAt(@PathVariable Long devolveId, @PathVariable Long id, @PathVariable LocalDate date) {
        Optional<RelatedCD4> related = devolveRepository.findById(devolveId).map(d -> {
            Laboratory laboratory = d.getRelatedCd4();
            RelatedCD4 relatedCD4 = null;
            if (laboratory != null) {
                /*List<LaboratoryLine> lines = laboratoryLineRepository.findByLaboratory(laboratory);*/
                Set<LaboratoryLine> lines = laboratory.getLines();
                for (LaboratoryLine line : lines) {
                    if (Long.valueOf(1L).equals(line.getLabTestId())) {
                        relatedCD4 = new RelatedCD4();
                        relatedCD4.setId(laboratory.getId());
                        relatedCD4.setDateResultReceived(laboratory.getDateResultReceived());
                        try {
                            relatedCD4.setValue(Double.parseDouble(line.getResult()));
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
            return Optional.ofNullable(relatedCD4);
        }).orElse(patientRepository.findById(id).flatMap(patient -> {
            List<Laboratory> laboratories = laboratoryRepository.findByPatientAndDateResultReceivedBefore(patient, date,
                    PageRequest.of(0, 3, Sort.by("dateResultReceived").descending()));
            for (Laboratory laboratory : laboratories) {
                /*List<LaboratoryLine> lines = laboratoryLineRepository.findByLaboratory(laboratory);*/
                Set<LaboratoryLine> lines = laboratory.getLines();
                for (LaboratoryLine line : lines) {
                    if (Long.valueOf(1L).equals(line.getLabTestId())) {
                        RelatedCD4 relatedCD4 = new RelatedCD4();
                        relatedCD4.setId(laboratory.getId());
                        relatedCD4.setDateResultReceived(laboratory.getDateResultReceived());
                        try {
                            relatedCD4.setValue(Double.parseDouble(line.getResult()));
                        } catch (Exception ignored) {
                        }
                        return Optional.of(relatedCD4);
                    }
                }
            }
            return Optional.empty();
        }));
        return ResponseUtil.wrapOrNotFound(related);
    }

    @GetMapping("/devolves/{devolveId}/patient/{id}/related-clinic/at/{date}")
    public ResponseEntity<RelatedClinic> getRelatedClinicByPatientAt(@PathVariable Long devolveId, @PathVariable Long id,
                                                                     @PathVariable LocalDate date) {
        Optional<RelatedClinic> related = devolveRepository.findById(devolveId).map(d -> {
            Clinic clinic = d.getRelatedClinic();
            RelatedClinic relatedClinic = null;
            if (clinic != null) {
                relatedClinic = new RelatedClinic();
                relatedClinic.setId(clinic.getId());
                clinic.setDateVisit(clinic.getDateVisit());
                clinic.setClinicStage(clinic.getClinicStage());
            }
            return Optional.ofNullable(relatedClinic);
        }).orElse(patientRepository.findById(id).flatMap(patient -> {
            List<Clinic> clinics = clinicRepository.findByPatientAndDateVisitBefore(patient, date,
                    PageRequest.of(0, 3, Sort.by("dateVisit").descending()));
            for (Clinic clinic : clinics) {
                if (StringUtils.isNotEmpty(clinic.getClinicStage())) {
                    RelatedClinic relatedClinic = new RelatedClinic();
                    relatedClinic.setId(clinic.getId());
                    relatedClinic.setDateVisit(clinic.getDateVisit());
                    relatedClinic.setClinicStage(clinic.getClinicStage());
                    return Optional.of(relatedClinic);
                }
            }
            return Optional.empty();
        }));
        return ResponseUtil.wrapOrNotFound(related);
    }
}
