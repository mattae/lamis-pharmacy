package org.fhi360.lamis.modules.pharmacy;

import com.foreach.across.core.annotations.AcrossDepends;
import com.foreach.across.core.context.configurer.ComponentScanConfigurer;
import com.foreach.across.modules.hibernate.jpa.AcrossHibernateJpaModule;
import org.lamisplus.modules.base.LamisModule;

@AcrossDepends(required = AcrossHibernateJpaModule.NAME)
public class LamisPharmacyModule extends LamisModule {
    public static final String NAME = "LAMISPharmacyModule";

    public LamisPharmacyModule() {
        super();
        addApplicationContextConfigurer(
                new ComponentScanConfigurer(getClass().getPackage().getName() + ".service",
                        getClass().getPackage().getName() + ".web"));
    }

    @Override
    public String getName() {
        return NAME;
    }
}
