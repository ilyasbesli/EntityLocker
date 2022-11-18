package com.andela.locker;


import java.io.Serializable;

public interface EntityLocker<T extends Serializable> {

    /**
     * This function locks the entity with the given T.
     *
     * @param entityT The T of the entity to lock.
     */
    void lockEntity(T entityT) throws InterruptedException;

    /**
     * Try to lock the entity with the given T for the given amount of time
     *
     * @param entityT The T of the entity to lock.
     * @param timeoutNanos The timeout in nanoseconds.
     * @return A boolean value indicating whether the entity was locked or not.
     */
    boolean lockEntity(T entityT, long timeoutNanos) throws InterruptedException;

    /**
     * Unlock the entity with the given T.
     *
     * @param entityT The T of the entity to unlock.
     */
    void unlockEntity(T entityT);
}
