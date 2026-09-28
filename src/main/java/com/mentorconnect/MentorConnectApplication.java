package com.mentorconnect;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class MentorConnectApplication {

    public static void main(String[] args) {
        SpringApplication.run(MentorConnectApplication.class, args);
    }
}
