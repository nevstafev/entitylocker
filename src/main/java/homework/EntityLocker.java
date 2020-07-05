package homework;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public class EntityLocker<ID> {
    Logger log = LoggerFactory.getLogger(EntityLocker.class);

    private ConcurrentMap<ID, ReentrantLock> locks = new ConcurrentHashMap<>();

    public <R> R doWithLock (ID id, Supplier<R> protectedCode) {
        return doWithLock(id, protectedCode, 0);
    }

    public <R> R doWithLock(ID id, Supplier<R> protectedCode, long timeOut) {
        lock(id, timeOut);
        R result = protectedCode.get();
        unlock(id);

        return result;
    }

    private void unlock(ID id) {
        ReentrantLock lock = locks.get(id);
        if (lock == null) {
            throw new RuntimeException("Entity id: " + id + " is not locked.");
        }
        lock.unlock();
        log.info("Successful unlock for {}", id);
    }

    private void lock(ID id, long timeout) {
        ReentrantLock lock = locks.computeIfAbsent(id, (entityId) -> new ReentrantLock());
        if (timeout > 0) {
            try {
                if (lock.tryLock() || lock.tryLock(timeout, TimeUnit.MILLISECONDS)) {
                    log.info("Successful got lock for {}", id);
                    return;
                }
            } catch (Exception e) {
                throw new RuntimeException("Can't lock entity id: " + id);
            }
            throw new RuntimeException("Can't lock entity id: " + id + ". Timeout is reached.");
        }
        lock.lock();
        log.info("Successful got lock for {}", id);
    }
}
