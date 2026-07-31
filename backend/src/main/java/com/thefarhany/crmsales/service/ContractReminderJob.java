package com.thefarhany.crmsales.service;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

@Component
public class ContractReminderJob extends QuartzJobBean {

    private static final Logger log = LoggerFactory.getLogger(ContractReminderJob.class);

    @Autowired
    private AiReminderService reminderService;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        log.info("===== Quartz Job: Contract Reminder Started =====");
        try {
            reminderService.processAutoReminders();
        } catch (Exception e) {
            log.error("Quartz Job Failed: {}", e.getMessage(), e);
            throw new JobExecutionException(e);
        }
        log.info("===== Quartz Job: Contract Reminder Finished =====");
    }
}
