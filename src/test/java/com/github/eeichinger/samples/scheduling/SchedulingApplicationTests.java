package com.github.eeichinger.samples.scheduling;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Slf4j
class SchedulingApplicationTests {

    @Autowired
    MockMvc mockMvc;

    @Test
    void singleInvocationAndStatus() throws Exception {
        mockMvc.perform(
				post("/start")
                        .with(user("user").password("password"))
                )
                .andExpect(status().is2xxSuccessful());
        log.info("received response success");
        log.info("waiting for long task to complete (check log)");
        Thread.sleep(100);
        mockMvc.perform(
                        get("/status")
                                .with(user("user").password("password"))
                )
                .andExpect(status().is2xxSuccessful());
        Thread.sleep(100);
        mockMvc.perform(
                        get("/status")
                                .with(user("user").password("password"))
                )
                .andExpect(status().is2xxSuccessful());
        Thread.sleep(1000); // wait for task to complete
        log.info("finished waiting - did it complete?");
    }

    @Test
    void duplicateInvocation() throws Exception {
        mockMvc.perform(
                        post("/start")
                                .with(user("user").password("password"))
                )
                .andExpect(status().is2xxSuccessful());
        mockMvc.perform(
                        post("/start")
                                .with(user("user").password("password"))
                )
                .andExpect(status().is2xxSuccessful());
        log.info("received response success - you should see the second request being reported in the logs as 'skipped'");
        log.info("waiting for long task to complete");
        Thread.sleep(1000);
        log.info("finished waiting - did it complete?");
    }
}
