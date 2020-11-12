package org.fhi360.lamis.modules.pharmacy.web.rest.vm;

import lombok.Data;
import org.lamisplus.modules.lamis.legacy.domain.entities.Drug;
import org.lamisplus.modules.lamis.legacy.domain.entities.RegimenDrug;

@Data
public class DrugDTO {
    private Drug drug;
    private RegimenDrug regimenDrug;

    public DrugDTO(RegimenDrug regimenDrug) {
        this.regimenDrug = regimenDrug;
        this.drug = regimenDrug.getDrug();
    }
}
