package test;

import java.util.LinkedList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Main {

    static long _10w = 10 * 10000;
    static AtomicInteger count = new AtomicInteger(0);
    static AtomicLong beforeTimeMillis = new AtomicLong(System.currentTimeMillis());

    public static void main(String[] args) {
        // write your code here
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(Main::thread1Print, 0, 1, TimeUnit.SECONDS);
        badCountedLoop();

    }

    /**
     * 线程1输出上次执行和当前执行之间的时间差
     */
    private static void thread1Print() {
        long currentTimeMillis = System.currentTimeMillis();
        System.out.printf("%s\t%s\n", count.addAndGet(1), (currentTimeMillis - beforeTimeMillis.get()));
        beforeTimeMillis.set(System.currentTimeMillis());

    }


    /**
     * 线程2构造大循环拖住GC
     */
    private static void badCountedLoop() {
        LinkedList<Integer> linkedList = new LinkedList<>();
        for (int i = 0; i < _10w; i++) {
            linkedList.add(Long.valueOf(i).intValue());
        }
        long lastMillis = System.currentTimeMillis();
        long count = 0L;

        while (true) {
            for (int i = 0; i < _10w; i++) {
                innerLoop(linkedList);
            }

            System.out.printf("%s\t%s\n", count, (System.currentTimeMillis() - lastMillis));
            count++;
            lastMillis = System.currentTimeMillis();

        }

    }

    private static void innerLoop(LinkedList<Integer> linkedList) {
        for (int i = 0; i < linkedList.size(); i++) {
            checkNull(linkedList.get(i));
        }
    }

    private static void checkNull(Object value) {
        // 无意义的空方法
    }


}


