package com.jobagent.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String JOB_DISCOVERY_QUEUE = "job.discovery";
    public static final String JOB_MATCHING_QUEUE = "job.matching";
    public static final String COVER_LETTER_QUEUE = "cover.letter.generation";
    public static final String APPLICATION_SUBMISSION_QUEUE = "application.submission";

    @Bean
    public Queue jobDiscoveryQueue() {
        return new Queue(JOB_DISCOVERY_QUEUE, true);
    }

    @Bean
    public Queue jobMatchingQueue() {
        return new Queue(JOB_MATCHING_QUEUE, true);
    }

    @Bean
    public Queue coverLetterQueue() {
        return new Queue(COVER_LETTER_QUEUE, true);
    }

    @Bean
    public Queue applicationSubmissionQueue() {
        return new Queue(APPLICATION_SUBMISSION_QUEUE, true);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
