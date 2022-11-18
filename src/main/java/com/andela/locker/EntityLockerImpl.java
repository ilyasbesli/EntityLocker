package com.andela.locker;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class EntityLockerImpl<T extends Serializable> implements EntityLocker<T> {

  private final ConcurrentMap<T, ReentrantLock> reentrantLockMap;

  public EntityLockerImpl() {
    this.reentrantLockMap = new ConcurrentHashMap<>();
  }

  /**
   * If the entity is not locked, lock it and return true. If the entity is locked, wait until it is unlocked and then lock it and return true.
   *
   * @param entityT The T of the entity to lock.
   */
  @Override
  public void lockEntity(T entityT) throws InterruptedException {
    lockEntity(entityT, -1);
  }

  /**
   * Locks an entity, if it is locked.
   *
   * @param entityId The T of the entity to lock.
   * @param timeoutNanos The timeout in nanoseconds. If the timeout is 0, the method will return immediately. If the timeout is negative, the method will wait indefinitely.
   * @return A boolean value.
   */
  @Override
  public boolean lockEntity(T entityId, long timeoutNanos) throws InterruptedException {
    if (entityId == null) {
      throw new AssertionError();
    }

    ReentrantLock reentrantLock = reentrantLockMap.computeIfAbsent(entityId, t -> new ReentrantLock());

    if (timeoutNanos >= 0) {
      return reentrantLock.tryLock(timeoutNanos, TimeUnit.NANOSECONDS);
    } else {
      reentrantLock.lockInterruptibly();
    }

    return true;
  }

  /**
   * If the lock is held by the current thread, remove the lock from the map if there are no other threads waiting for it, and then unlock the lock
   *
   * @param entityId The T of the entity to lock.
   */
  @Override
  public void unlockEntity(T entityId) {
    ReentrantLock lock = reentrantLockMap.get(entityId);
    if (lock != null) {
      if (!lock.isHeldByCurrentThread()) {
        throw new IllegalMonitorStateException();
      }
      if (!lock.hasQueuedThreads()) {
        reentrantLockMap.remove(entityId);
      }
      lock.unlock();
    }
  }
}
