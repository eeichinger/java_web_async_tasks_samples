package com.github.eeichinger.samples.scheduling;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
class MyLongRunningProcess implements Runnable {
    static final AtomicReference<MyLongRunningProcess> activeProcess = new AtomicReference<>();

    public static MyLongRunningProcess getActiveProcess() {
        return activeProcess.get();
    }

    // must be volatile because it's being read from web threads and modified by worker thread
    private final AtomicInteger count = new AtomicInteger();

    public int getCount() {
        return count.get();
    }

    @SneakyThrows
    public void run() {
        // register as active worker
        if (!activeProcess.compareAndSet(null, this)) {
            log.warn("duplicate job invocation, skipping task");
            return;
        }

        try {
            for (int i = 0; i < 50; i++) {
                count.set(i); // report progress
                Thread.sleep(50); // simulate work
            }
        } finally {
            // clear active worker
            activeProcess.set(null);
        }
    }
}
