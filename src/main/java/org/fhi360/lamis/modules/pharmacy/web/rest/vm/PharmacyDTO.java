package org.fhi360.lamis.modules.pharmacy.web.rest.vm;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import org.lamisplus.modules.lamis.legacy.domain.entities.Facility;
import org.lamisplus.modules.lamis.legacy.domain.entities.Patient;
import org.lamisplus.modules.lamis.legacy.domain.entities.PharmacyAdverseDrugReaction;
import org.lamisplus.modules.lamis.legacy.domain.entities.PharmacyLine;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
public class PharmacyDTO {
    private Long id;
    private Facility facility;
    private Patient patient;
    private Boolean adrScreened;
    private Boolean adherence;
    private LocalDate dateVisit;
    private String mmdType;
    private Integer duration;
    private Boolean prescriptionError;
    private LocalDate nextAppointment;
    private JsonNode extra;
    String uuid;
    private Set<PharmacyLine> lines = new HashSet<>();
    private List<PharmacyAdverseDrugReaction> adrs = new ArrayList<>();
}
