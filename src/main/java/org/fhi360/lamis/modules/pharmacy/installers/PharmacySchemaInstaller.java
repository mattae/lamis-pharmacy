package org.fhi360.lamis.modules.pharmacy.installers;

import com.foreach.across.core.annotations.Installer;
import com.foreach.across.core.installers.AcrossLiquibaseInstaller;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;

@Order(1)
@Installer(name = "pharmacy-schema-installer-4", description = "Installs the required database tables", version = 11)
@Slf4j
public class PharmacySchemaInstaller extends AcrossLiquibaseInstaller {
    public PharmacySchemaInstaller() {
        super("classpath:installers/pharmacy/schema/schema.xml");
        LOG.info("Pharmacy schema installer");
    }
}
