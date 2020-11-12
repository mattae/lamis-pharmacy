package org.fhi360.lamis.modules.pharmacy.web.rest.vm;

import lombok.Data;
import org.lamisplus.modules.lamis.legacy.domain.entities.Drug;
import org.lamisplus.modules.lamis.legacy.domain.entities.Regimen;
import org.lamisplus.modules.lamis.legacy.domain.entities.RegimenDrug;
import org.lamisplus.modules.lamis.legacy.domain.entities.RegimenType;

@Data
public class PharmacyLineDTO {
    private Long id;
    private String description;
    private Double morning;
    private Double afternoon;
    private Double evening;
    private Integer duration;
    private RegimenType regimenType;
    private Regimen regimen;
    private RegimenDrug regimenDrug;
    private Drug drug;
}
