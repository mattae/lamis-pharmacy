package org.fhi360.lamis.modules.pharmacy.web.rest.vm;

import lombok.Data;

import java.time.LocalDate;

@Data
public class RelatedClinic {
    private Long id;
    private String clinicStage;
    private LocalDate dateVisit;
}
