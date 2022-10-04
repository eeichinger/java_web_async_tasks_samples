package com.github.eeichinger.samples.scheduling;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.concurrent.DelegatingSecurityContextRunnable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.*;

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

    @SneakyThrows
    public AsyncWorker(TenantService tenantService, Runnable task) {
        this.tenantService = tenantService;
        this.currentTenantId = tenantService.getCurrentTenantId(); // capture tenant from current thread
        SecurityContext securityContext = cloneObject(SecurityContextHolder.getContext(), SecurityContext.class);
        // override only for logging
        this.task = new DelegatingSecurityContextRunnable(() -> {
            log.info("executing important process on behalf of tenant {} and user '{}/'", currentTenantId, securityContext.getAuthentication());
            task.run(); // do the work
            log.info("executing process complete");
        }, securityContext);

        log.info("created new job instance for user {}", securityContext.getAuthentication());
    }

    private static <T> T cloneObject(T object, Class<T> clazz) {
        byte[] data = serializeObject(object);
        return deserializeObject(data, clazz);
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    private static <T> T deserializeObject(byte[] data, Class<T> clazz) {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        ObjectInputStream objectInputStream = new ObjectInputStream(bais);
        Object o = objectInputStream.readObject();
        objectInputStream.close();
        return (T)o;
    }

    @SneakyThrows
    private static byte[] serializeObject(Object securityContext) {
        ByteArrayOutputStream baus = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(baus);
        objectOutputStream.writeObject(securityContext);
        objectOutputStream.flush();
        objectOutputStream.close();
        return baus.toByteArray();
    }

    @Override
    @SneakyThrows
    public void run() {
        tenantService.runAsTenant(currentTenantId, () -> {
            task.run(); // do the work
        });
    }
}
