package com.sipe.slack.helping;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class HelpingApplication {

    public static void main(String[] args) {
        SpringApplication.run(HelpingApplication.class, args);
    }

}