package com.andela.locker;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.math.BigDecimal;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class EntityLockerTests {

    private ExecutorService executor ;

    @BeforeAll
    public void setUp() {
        executor = Executors.newFixedThreadPool(2);
    }

    @AfterAll
    public void tearDown() {
        executor.shutdown();
    }

    @Test
    public void testOneThreadOneEntity() throws Exception {
        EntityLocker<Long> locker = new EntityLockerImpl<>();
        Long entityId = 1L;
        locker.lockEntity(entityId);
        locker.unlockEntity(entityId);
    }

    @Test
    public void testTwoThreadsOneEntityNoExceptions() throws Exception {
        EntityLocker<String> locker = new EntityLockerImpl<>();
        String entityId = "entityId";
        Runnable runnable = () -> {
            try {
                locker.lockEntity(entityId);
                locker.unlockEntity(entityId);
            } catch (InterruptedException e) {
                log.error(e.getMessage(), e);
            }
        };
        Future<?> task1 = executor.submit(runnable);
        Future<?> task2 = executor.submit(runnable);

        task1.get();
        task2.get();
    }

    @Test()
    public void testWaitOtherThread() {
        EntityLocker<BigDecimal> locker = new EntityLockerImpl<>();
        BigDecimal entityId = BigDecimal.TEN;
        assertThrows(TimeoutException.class, () -> {
            locker.lockEntity(entityId);
            Future<?> task1 = executor.submit(() -> {
                try {
                    locker.lockEntity(entityId);
                } catch (InterruptedException e) {
                    log.error("Thread interrupted", e);
                }
                locker.unlockEntity(entityId);
            });

            try {
                task1.get(1, TimeUnit.SECONDS);
            } finally {
                task1.cancel(true);
                locker.unlockEntity(entityId);
            }
        });
    }

    @Test
    public void testLockInDifferentEntityIds() throws Exception {
        EntityLocker<Integer> locker = new EntityLockerImpl<>();
        Integer entityId1 = Integer.MAX_VALUE;
        Integer entityId2 = Integer.MIN_VALUE;

        locker.lockEntity(entityId1);
        Future<?> task1 = executor.submit(() -> {
            try {
                locker.lockEntity(entityId2);
                locker.unlockEntity(entityId2);
            } catch (InterruptedException e) {
                log.error(e.getMessage(), e);
            }
        });
        task1.get(100, TimeUnit.MILLISECONDS);
        locker.unlockEntity(entityId1);
    }

    @Test
    public void testLockWithTimeout() throws Exception {
        EntityLocker<String> locker = new EntityLockerImpl<>();
        String entityId = "entityIdValueExample";

        locker.lockEntity(entityId);
        try {
            Future<Boolean> task1 = executor.submit(() -> {
                try {
                    boolean locked = locker.lockEntity(entityId, 100000);
                    if (locked) locker.unlockEntity(entityId);
                    return locked;
                } catch (InterruptedException e) {
                    log.error(e.getMessage(), e);
                    return true;
                }
            });
            assertFalse(task1.get());
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        } finally {
            locker.unlockEntity(entityId);
        }
    }

    @Test
    public void testReentryLock() throws Exception {
        EntityLocker<Long> locker = new EntityLockerImpl<>();
        Long entityId = 100L;

        locker.lockEntity(entityId);
        locker.unlockEntity(entityId);

        locker.lockEntity(entityId);
        locker.lockEntity(entityId, 10);

        Future<Boolean> task1 = executor.submit(() -> {
            try {
                return locker.lockEntity(entityId, 100000);
            } catch (InterruptedException e) {
                log.error(e.getMessage(), e);
                return true;
            }
        });
        assertFalse(task1.get());
        locker.unlockEntity(entityId);

    }
}