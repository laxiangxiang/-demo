import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by LXX on 2019/4/1.
 * volatile并不保证原子性，并且counter++也不是原子操作（包含三步操作），
 * 所以结果总是小于10*10000.
 * 解决办法：使用synchronized，lock或者原子类
 */
public class VolatileExample {
    private static volatile int counter = 0;
    private static final AtomicInteger atomicInteger = new AtomicInteger(0);
    private static final Lock lock = new ReentrantLock();
    private static int counter2 = 0;
    private static final int threadCount = 10;
    private static CountDownLatch latch;
    private static final Object waiteObj = new Object();
    public static void main(String[] args) {
        latch = new CountDownLatch(threadCount);
        for (int i = 0;i < threadCount ; i++){
            Thread thread = new Thread(new Runnable() {
                public void run() {
                    for (int j = 0;j < 10000;j++){
                        counter++;
                    }
                    latch.countDown();
                }
            });
            thread.start();
        }
        try {
            latch.await();
        }catch (InterruptedException e){
            e.printStackTrace();
        }
        System.out.println(counter);
        useAtomicInteger();
        useLock();
        useSynchronized();
    }

    public static void useAtomicInteger(){
        latch = new CountDownLatch(threadCount);
        for (int i = 0;i < threadCount;i++){
            Thread thread = new Thread(new Runnable() {
                public void run() {
                    for (int j = 0;j < 10000;j++){
                        atomicInteger.incrementAndGet();
                    }
                    latch.countDown();
                }
            });
            thread.start();
        }
        try {
            latch.await();
        }catch (InterruptedException e){
            e.printStackTrace();
        }
        System.out.println(atomicInteger.get());
    }

    public static void useLock(){
        latch = new CountDownLatch(threadCount);
        counter2 = 0;
        for (int i = 0;i < threadCount;i++){
            Thread thread = new Thread(new Runnable() {
                public void run() {
                    lock.lock();
                    for (int j = 0;j < 10000;j++){
                        counter2++;
                    }
                    lock.unlock();
                    latch.countDown();
                }
            });
            thread.start();
        }
        try {
            latch.await();
        }catch (InterruptedException e){
            e.printStackTrace();
        }
        System.out.println(counter2);
    }

    public static void useSynchronized(){
        latch = new CountDownLatch(threadCount);
        counter2 = 0;
        for (int i = 0;i < threadCount;i++){
            Thread thread = new Thread(new Runnable() {
                public void run() {
                    synchronized (waiteObj){
                        for (int j = 0;j < 10000;j++){
                            counter2++;
                        }
                    }
                    latch.countDown();
                }
            });
            thread.start();
        }
        try {
            latch.await();
        }catch (InterruptedException e){
            e.printStackTrace();
        }
        System.out.println(counter2);
    }
}
