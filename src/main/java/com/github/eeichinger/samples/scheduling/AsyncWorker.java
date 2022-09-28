package com.github.eeichinger.samples.scheduling;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * This class captures the (security) context of the thread that creates an instance (see constructor).
 * Once scheduled with e.g. a ThreadPoolTaskScheduler, the run() method will be invoked on the worker thread and apply
 * the security context to the worker thread before executing the actual work.
 * It also ensures, that the worker thread is cleaned up after completing the work.
 * Further it ensures that there can only 1 AsyncWorker instance be active at a given time.
 */
@Slf4j
public class AsyncWorker implements Runnable {

    SecurityContext securityContext;
    final TenantService tenantService;
    final String currentTenantId;
    final Runnable task;

    static final AtomicReference<AsyncWorker> activeWorker = new AtomicReference<AsyncWorker>();

    public static AtomicReference<AsyncWorker> getActiveWorker() {
        return activeWorker;
    }

    public AsyncWorker(TenantService tenantService, Runnable task) {
        this.tenantService = tenantService;
        this.currentTenantId = tenantService.getCurrentTenantId(); // capture tenant from current thread
        this.task = task; // what we're supposed to to
        securityContext = SecurityContextHolder.getContext(); // capture securitycontext from current thread
        log.info("created new job instance for user {}", securityContext);
    }

    @Override
    @SneakyThrows
    public void run() {
        try {
            // register as active worker
            if (!activeWorker.compareAndSet(null, this)) {
                log.warn("duplicate job invocation, skipping task");
                return;
            }
            // apply security context on worker thread
            SecurityContextHolder.setContext(securityContext);
            tenantService.runAsTenant( currentTenantId, ()->{
                log.info("executing important process on behalf of tenant {} and user '{}/'", currentTenantId, securityContext);
                task.run(); // do the work
                log.info("executing process complete");
            });
        } finally {
            // clear active worker
            activeWorker.set(null);
            // clean up security context on worker thread
            SecurityContextHolder.clearContext();
        }
    }
}
