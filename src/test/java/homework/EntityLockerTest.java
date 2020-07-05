package homework;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class EntityLockerTest {

    @Test
    public void testMultiplyLocksForOneEntity() throws Exception {
        EntityLocker<String> entityLocker = new EntityLocker<>();
        Entity entity = new Entity("1");

        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            threads.add(new Thread(() -> {
                entityLocker.doWithLock(entity.getId(), () -> {
                    sleep(1);
                    entity.setValue(entity.getValue() + 1);
                    return null;
                });
            }));
        }
        threads.forEach(Thread::start);
        for (Thread thread : threads) {
            thread.join();
        }

        Assertions.assertEquals(10, entity.getValue());
    }

    @Test
    public void testExecuteProtectedCodeOfDifferentEntities() throws Exception {
        Entity first = new Entity("1");
        Entity second = new Entity("2");

        EntityLocker<String> entityLocker = new EntityLocker<>();

        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            threads.add(new Thread(() -> {
                entityLocker.doWithLock(first.getId(), () -> {
                    sleep(1);
                    first.setValue(first.getValue() + 1);
                    return null;
                });
            }));
            threads.add(new Thread(() -> {
                entityLocker.doWithLock(second.getId(), () -> {
                    sleep(1);
                    second.setValue(second.getValue() + 1);
                    return null;
                });
            }));
        }
        threads.forEach(Thread::start);
        for (Thread thread : threads) {
            thread.join();
        }

        Assertions.assertEquals(10, first.getValue());
        Assertions.assertEquals(10, second.getValue());

    }

    @Test
    public void testWaitingForLockWithTimeout() throws Exception {
        Entity entity = new Entity("1");
        EntityLocker<String> entityLocker = new EntityLocker<>();

        Thread firstThread = new Thread(() -> entityLocker.doWithLock(entity.getId(), () -> {
            sleep(100);
            entity.setValue(1);
            return null;
        }));
        Thread secondThread = new Thread(() ->
                Assertions.assertThrows(RuntimeException.class, () -> entityLocker.doWithLock(entity.getId(), () -> {
                    entity.setValue(2);
                    return null;
                }, 1)));

        firstThread.start();
        secondThread.start();
        firstThread.join();
        secondThread.join();

        Assertions.assertEquals(1, entity.getValue());
    }

    private void sleep(long milliseconds) {
        try {
            TimeUnit.MILLISECONDS.sleep(milliseconds);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}