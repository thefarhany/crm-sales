package com.thefarhany.crmsales.config;

import com.thefarhany.crmsales.service.ContractReminderJob;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {

    @Bean
    public JobDetail reminderJobDetail() {
        return JobBuilder.newJob(ContractReminderJob.class)
                .withIdentity("contractReminderJob", "reminder-group")
                .storeDurably()
                .requestRecovery(true)
                .build();
    }

    @Bean
    public Trigger reminderTrigger(JobDetail reminderJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(reminderJobDetail)
                .withIdentity("contractReminderTrigger", "reminder-group")
                .withSchedule(CronScheduleBuilder
                        .cronSchedule("0 0 8 * * ?")
                        .withMisfireHandlingInstructionFireAndProceed())
                .build();
    }
}
