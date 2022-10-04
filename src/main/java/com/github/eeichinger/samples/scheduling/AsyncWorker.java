package com.github.eeichinger.samples.scheduling;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SerializationUtils;
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

    final TenantService tenantService;
    final String currentTenantId;
    final Runnable task;

    public AsyncWorker(TenantService tenantService, Runnable task) {
        this.tenantService = tenantService;
        this.currentTenantId = tenantService.getCurrentTenantId(); // capture tenant from current thread
        SecurityContext securityContext = SerializationUtils.clone(SecurityContextHolder.getContext());
        // override only for logging
        this.task = new DelegatingSecurityContextRunnable(() -> {
            log.info("executing important process on behalf of tenant {} and user '{}/'", currentTenantId, securityContext.getAuthentication());
            task.run(); // do the work
            log.info("executing process complete");
        }, securityContext);

        log.info("created new job instance for user {}", securityContext.getAuthentication());
    }

    @Override
    public void run() {
        tenantService.runAsTenant(currentTenantId, () -> {
            task.run(); // do the work
        });
    }
}
