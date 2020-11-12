package org.fhi360.lamis.modules.pharmacy.service;

import lombok.extern.slf4j.Slf4j;
import org.lamisplus.modules.base.config.ContextProvider;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

@Slf4j
public class LostToFollowupJob implements Job {
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        LOG.debug("Running Lost to followup job...");
        ContextProvider.getBean(PharmacyService.class).updateLostToFollowup();
    }
}
