package org.fhi360.lamis.modules.pharmacy.service.widget;

import lombok.SneakyThrows;
import org.fhi360.lamis.modules.patient.service.providers.PatientWidgetProvider;
import org.lamisplus.modules.lamis.legacy.domain.entities.Patient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

//@Service
public class LatestPharmacyWidget implements PatientWidgetProvider {
    @Override
    public String getTitle() {
        return "Latest Refill Info";
    }

    @Override
    public String getComponentName() {
        return "PharmacyWidget";
    }

    @Override
    public String getModuleName() {
        return "PharmacyWidgetModule";
    }

    @SneakyThrows
    @Override
    public String getUmdContent() {
        Resource resource = new ClassPathResource("views/static/pharmacy/js/bundles/lamis-pharmacy-1.0.1.umd.js");
        return new String(FileCopyUtils.copyToByteArray(resource.getInputStream()));
    }

    @Override
    public String getUrl() {
        return "/across/resources/static/pharmacy/js/bundles/lamis-pharmacy-1.0.1.umd.js";
    }

    @Override
    public boolean applicableTo(Patient patient) {
        return true;
    }
}
