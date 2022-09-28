package com.github.eeichinger.samples.scheduling;

import org.springframework.stereotype.Service;

// just a dummy implementation of the real TenantService
@Service
public class TenantService {

    String getCurrentTenantId() {
        return "someTenantId";
    }
    void runAsTenant(String tenantId, Runnable job) {
        job.run();
    }
}
