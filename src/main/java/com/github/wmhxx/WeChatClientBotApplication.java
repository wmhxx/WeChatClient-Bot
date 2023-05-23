package com.github.wmhxx;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class WeChatClientBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(WeChatClientBotApplication.class, args);
    }

}
