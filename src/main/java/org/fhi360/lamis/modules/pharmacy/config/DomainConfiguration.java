package org.fhi360.lamis.modules.pharmacy.config;

import com.foreach.across.modules.hibernate.jpa.repositories.config.EnableAcrossJpaRepositories;
import org.fhi360.lamis.modules.pharmacy.domain.PharmacyDowmain;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableAcrossJpaRepositories(basePackageClasses = {PharmacyDowmain.class})
public class DomainConfiguration {
}
