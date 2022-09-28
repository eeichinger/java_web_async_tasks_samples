package com.github.eeichinger.samples.scheduling;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.concurrent.DelegatingSecurityContextRunnable;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * This class captures the (security) context of the thread that creates an instance (see constructor).
 * Once scheduled with e.g. a ThreadPoolTaskScheduler, the run() method will be invoked on the worker thread and apply
 * the security context to the worker thread before executing the actual work.
 * It also ensures, that the worker thread is cleaned up after completing the work.
 */
@Slf4j
public class AsyncWorker implements Runnable {

    final SecurityContext securityContext;
    final TenantService tenantService;
    final String currentTenantId;
    final Runnable task;

    public AsyncWorker(TenantService tenantService, Runnable task) {
        this.tenantService = tenantService;
        this.currentTenantId = tenantService.getCurrentTenantId(); // capture tenant from current thread
        this.task = new DelegatingSecurityContextRunnable(task); // handle Spring SecurityContext
        this.securityContext = SecurityContextHolder.getContext(); // only for logging - context switching is handled by DelegatingSecurityContextRunnable
        log.info("created new job instance for user {}", securityContext);
    }

    @Override
    @SneakyThrows
    public void run() {
        tenantService.runAsTenant( currentTenantId, ()->{
            log.info("executing important process on behalf of tenant {} and user '{}/'", currentTenantId, securityContext);
            task.run(); // do the work
            log.info("executing process complete");
        });
    }
}
