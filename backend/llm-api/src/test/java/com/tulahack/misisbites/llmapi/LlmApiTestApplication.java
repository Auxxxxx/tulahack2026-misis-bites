package com.tulahack.misisbites.llmapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.TestConfiguration;

@SpringBootApplication
public class LlmApiTestApplication {

    public static void main(String[] args) {
        SpringApplication.from(LlmApiTestApplication::main).with(LlmApiTestApplication.class).run(args);
    }
}
