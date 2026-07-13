package com.jobagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class JobApplicationAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(JobApplicationAgentApplication.class, args);
    }
}
