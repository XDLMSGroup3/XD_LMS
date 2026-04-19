package com.group3.xd_lms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // 开启定时任务支持
public class XdLmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(XdLmsApplication.class, args);
    }

}
