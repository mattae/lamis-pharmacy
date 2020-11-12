package org.fhi360.lamis.modules.pharmacy.web.rest.vm;

import lombok.Data;

import java.time.LocalDate;

@Data
public class RelatedViralLoad {
    private Long id;
    private Double value;
    private LocalDate dateResultReceived;
}
