package org.fhi360.lamis.modules.pharmacy.web.rest.vm;

import lombok.Data;

import java.time.LocalDate;

@Data
public class RelatedPharmacy {
    private Long id;
    private String regimen;
    private LocalDate dateVisit;
}
