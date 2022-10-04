package com.github.eeichinger.samples.scheduling;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
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
        MyLongRunningProcess process = new MyLongRunningProcess();
        AsyncWorker job = new AsyncWorker(tenantService, process);
        log.info("scheduling async process");
        taskScheduler.schedule(job, new Date(System.currentTimeMillis()));
        // request will complete, job will execute on a separate thread
        // simulate clearing Authentication at end of request
        SecurityContextHolder.getContext().setAuthentication(null);
    }

    @GetMapping("/status")
    public ResponseEntity<Integer> processStatus() {
        MyLongRunningProcess activeProcess = MyLongRunningProcess.getActiveProcess();
        if (activeProcess == null) {
            log.info("reporting progress status - no active process");
            return ResponseEntity.status(HttpStatus.GONE).build();
        }
        int count = activeProcess.getCount();
        log.info("reporting progress status count={}", count);
        return ResponseEntity.status(HttpStatus.OK).body(count);
    }

}
