package com.github.eeichinger.samples.scheduling;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

@RestController
@Slf4j
public class MyController {

    final TaskScheduler taskScheduler;
    final TenantService tenantService;

    @Autowired
    public MyController(@NonNull TaskScheduler taskScheduler, @NonNull TenantService tenantService) {
        this.taskScheduler = taskScheduler;
        this.tenantService = tenantService;
    }

    @PostMapping("/start")
    public void startProcess() {
        AsyncWorker job = new AsyncWorker(tenantService, ()->process());
        log.info("scheduling async process");
        taskScheduler.schedule(job, new Date(System.currentTimeMillis()));
    }

    @SneakyThrows
    private void process() {
        Thread.sleep(500); // simulate work
    }
}
