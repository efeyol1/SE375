package SE375_LAB_PRO;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.*;

// SE375 - Lab Assignment
// Producer Consumer problem with two different lock types

public class Producerconsumerlab {

    // ---- shared variables for version 1 ----
    static LinkedList<Integer> buffer1 = new LinkedList<>();
    static int bufferSize;

    static ReentrantLock lock = new ReentrantLock();
    static Condition notFull  = lock.newCondition();
    static Condition notEmpty = lock.newCondition();

    static int produced1 = 0;
    static int consumed1 = 0;
    static int lockAcquired = 0;
    static int lockReleased = 0;

    // ---- shared variables for version 2 ----
    static LinkedList<Integer> buffer2 = new LinkedList<>();

    static ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    static Lock rLock = rwLock.readLock();
    static Lock wLock = rwLock.writeLock();

    // need a separate condition lock because readwritelock doesnt support conditions
    static ReentrantLock condLock2 = new ReentrantLock();
    static Condition notFull2  = condLock2.newCondition();
    static Condition notEmpty2 = condLock2.newCondition();

    static int produced2 = 0;
    static int consumed2 = 0;
    static int readAcquired  = 0;
    static int readReleased  = 0;
    static int writeAcquired = 0;
    static int writeReleased = 0;

    // item counter to give each produced item a unique number
    static AtomicInteger itemCounter = new AtomicInteger(0);

    // how many items each thread should handle
    static int itemsPerThread;


    // VERSION 1 - Producer thread (ReentrantLock)

    static class Producer1 extends Thread {
        public void run() {
            for (int i = 0; i < itemsPerThread; i++) {
                try {
                    lock.lock();
                    lockAcquired++;

                    // wait if buffer is full
                    while (buffer1.size() == bufferSize) {
                        notFull.await();
                    }

                    int item = itemCounter.incrementAndGet();
                    buffer1.add(item);
                    produced1++;

                    notEmpty.signalAll();

                    lockReleased++;
                    lock.unlock();

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    // VERSION 1 - Consumer thread (ReentrantLock)

    static class Consumer1 extends Thread {
        public void run() {
            for (int i = 0; i < itemsPerThread; i++) {
                try {
                    lock.lock();
                    lockAcquired++;

                    // wait if buffer is empty
                    while (buffer1.isEmpty()) {
                        notEmpty.await();
                    }

                    buffer1.removeFirst();
                    consumed1++;

                    notFull.signalAll();

                    lockReleased++;
                    lock.unlock();

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    // VERSION 2 - Producer thread (ReadWriteLock)

    static class Producer2 extends Thread {
        public void run() {
            for (int i = 0; i < itemsPerThread; i++) {
                try {
                    condLock2.lock();

                    // check size using read lock
                    rLock.lock();
                    readAcquired++;
                    int sz = buffer2.size();
                    readReleased++;
                    rLock.unlock();

                    while (sz == bufferSize) {
                        notFull2.await();

                        rLock.lock();
                        readAcquired++;
                        sz = buffer2.size();
                        readReleased++;
                        rLock.unlock();
                    }

                    // add item using write lock
                    wLock.lock();
                    writeAcquired++;
                    buffer2.add(itemCounter.incrementAndGet());
                    produced2++;
                    writeReleased++;
                    wLock.unlock();

                    notEmpty2.signalAll();
                    condLock2.unlock();

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // VERSION 2 - Consumer thread (ReadWriteLock)

    static class Consumer2 extends Thread {
        public void run() {
            for (int i = 0; i < itemsPerThread; i++) {
                try {
                    condLock2.lock();

                    // check size with read lock
                    rLock.lock();
                    readAcquired++;
                    int sz = buffer2.size();
                    readReleased++;
                    rLock.unlock();

                    while (sz == 0) {
                        notEmpty2.await();

                        rLock.lock();
                        readAcquired++;
                        sz = buffer2.size();
                        readReleased++;
                        rLock.unlock();
                    }

                    // remove item using write lock
                    wLock.lock();
                    writeAcquired++;
                    buffer2.removeFirst();
                    consumed2++;
                    writeReleased++;
                    wLock.unlock();

                    notFull2.signalAll();
                    condLock2.unlock();

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // reset all the stats before each run
    static void resetVersion1() {
        buffer1.clear();
        produced1    = 0;
        consumed1    = 0;
        lockAcquired = 0;
        lockReleased = 0;
        itemCounter.set(0);
        lock     = new ReentrantLock();
        notFull  = lock.newCondition();
        notEmpty = lock.newCondition();
    }

    static void resetVersion2() {
        buffer2.clear();
        produced2     = 0;
        consumed2     = 0;
        readAcquired  = 0;
        readReleased  = 0;
        writeAcquired = 0;
        writeReleased = 0;
        itemCounter.set(0);
        rwLock    = new ReentrantReadWriteLock();
        rLock     = rwLock.readLock();
        wLock     = rwLock.writeLock();
        condLock2 = new ReentrantLock();
        notFull2  = condLock2.newCondition();
        notEmpty2 = condLock2.newCondition();
    }

    // run version 1 with given producer/consumer count
    static void runVersion1(int numProducers, int numConsumers, int totalItems) throws InterruptedException {
        resetVersion1();
        itemsPerThread = totalItems / numProducers;

        Thread[] producers = new Thread[numProducers];
        Thread[] consumers = new Thread[numConsumers];

        for (int i = 0; i < numProducers; i++) producers[i] = new Producer1();
        for (int i = 0; i < numConsumers; i++) consumers[i] = new Consumer1();

        long startTime = System.currentTimeMillis();

        for (Thread t : producers) t.start();
        for (Thread t : consumers) t.start();
        for (Thread t : producers) t.join();
        for (Thread t : consumers) t.join();

        long time = System.currentTimeMillis() - startTime;

        System.out.println("ReentrantLock");
        System.out.println("Produced: " + produced1);
        System.out.println("Consumed: " + consumed1);
        System.out.println("Lock acquired: " + lockAcquired);
        System.out.println("Lock released: " + lockReleased);
        System.out.println("Execution time: " + time + " ms");
    }

    // run version 2 with given producer/consumer count
    static void runVersion2(int numProducers, int numConsumers, int totalItems) throws InterruptedException {
        resetVersion2();
        itemsPerThread = totalItems / numProducers;

        Thread[] producers = new Thread[numProducers];
        Thread[] consumers = new Thread[numConsumers];

        for (int i = 0; i < numProducers; i++) producers[i] = new Producer2();
        for (int i = 0; i < numConsumers; i++) consumers[i] = new Consumer2();

        long startTime = System.currentTimeMillis();

        for (Thread t : producers) t.start();
        for (Thread t : consumers) t.start();
        for (Thread t : producers) t.join();
        for (Thread t : consumers) t.join();

        long time = System.currentTimeMillis() - startTime;

        System.out.println("ReadWriteLock");
        System.out.println("Produced: " + produced2);
        System.out.println("Consumed: " + consumed2);
        System.out.println("Read lock acquired: " + readAcquired);
        System.out.println("Read lock released: " + readReleased);
        System.out.println("Write lock acquired: " + writeAcquired);
        System.out.println("Write lock released: " + writeReleased);
        System.out.println("Execution time: " + time + " ms");
    }

    public static void main(String[] args) throws InterruptedException {
        Scanner sc = new Scanner(System.in);

        System.out.print("Buffer size: ");
        bufferSize = sc.nextInt();

        System.out.print("Number of producers: ");
        int numProducers = sc.nextInt();

        System.out.print("Number of consumers: ");
        int numConsumers = sc.nextInt();

        System.out.print("Total items to produce: ");
        int totalItems = sc.nextInt();

        System.out.println();

        // run both versions with user input first
        System.out.println("--- Version 1 ---");
        runVersion1(numProducers, numConsumers, totalItems);
        System.out.println();
        System.out.println("--- Version 2 ---");
        runVersion2(numProducers, numConsumers, totalItems);

        System.out.println();
        System.out.println("--- Experiment Runs (fixed 10000 items) ---");
        System.out.println();

        // different configs as required by the lab
        int[][] configs = { {1,1}, {2,2}, {4,4}, {8,8} };
        int expItems = 10000;

        for (int[] cfg : configs) {
            int p = cfg[0];
            int c = cfg[1];

            // make sure items is divisible
            int items = (expItems / p) * p;

            System.out.println("Producers: " + p + " | Consumers: " + c);
            runVersion1(p, c, items);
            System.out.println();
            runVersion2(p, c, items);
            System.out.println("------------------------------");
            System.out.println();
        }

        sc.close();
    }
}