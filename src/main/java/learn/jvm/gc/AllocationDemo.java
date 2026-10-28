package learn.jvm.gc;

import java.util.ArrayList;
import java.util.List;

/**
 * 对象分配与可见性演示：线程本地分配缓冲 (TLAB)、逃逸分析的直观感受、以及
 * "为什么不能用标志位做线程间停止"。
 *
 * 运行：
 *   java -cp target/classes -Xms20m -Xmx20m -verbose:gc learn.jvm.gc.AllocationDemo
 *   java -cp target/classes -Xms20m -Xmx20m -XX:+UseG1GC -Xlog:gc* \
 *        learn.jvm.gc.AllocationDemo
 *
 * 观察点：
 *   1. 大量小对象分配不一定触发 GC（TLAB、逃逸分析消除分配）
 *   2. 线程间靠 volatile/锁保证可见性；普通 boolean 标志会"卡住"
 */
public class AllocationDemo {

    // 逃逸分析：对象没有逃出方法，可能被 JIT 标量替换，根本不分配在堆上
    static int escapeAnalysis() {
        int sum = 0;
        for (int i = 0; i < 100_000; i++) {
            Point p = new Point(i, i);
            sum += p.x + p.y;
        }
        return sum; // p 从未逃逸出循环/方法
    }

    static class Point {
        final int x, y;
        Point(int x, int y) { this.x = x; this.y = y; }
    }

    // 可见性实验：普通字段，JIT 可能把 flag 提升到寄存器，导致循环不退出
    static boolean stopFlag = false;

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== 1. 大量小对象分配 ===");
        long t0 = System.nanoTime();
        int sum = escapeAnalysis();
        System.out.println("escapeAnalysis() = " + sum
                + "，耗时 " + (System.nanoTime() - t0) / 1_000_000 + " ms（几乎无 GC）");

        System.out.println("\n=== 2. 真实分配触发 GC ===");
        List<byte[]> keep = new ArrayList<>();
        try {
            for (int i = 0; i < 2000; i++) {
                keep.add(new byte[1024]); // 2000 * 1KB = 2MB，20m 堆里会多次 YGC
            }
            System.out.println("分配完成，共 " + keep.size() + " 块 1KB");
        } catch (OutOfMemoryError e) {
            System.out.println("OutOfMemoryError（keep 把堆占满了）: " + e.getMessage());
        }

        System.out.println("\n=== 3. 线程可见性 ===");
        stopFlag = false;
        Thread worker = new Thread(() -> {
            int i = 0;
            while (!stopFlag) { i++; }  // 可能因指令重排/寄存器缓存永远看不到 true
            System.out.println("worker 正常退出，i=" + i);
        });
        worker.start();
        Thread.sleep(100);
        stopFlag = true; // 主线程写
        worker.join(500);
        if (worker.isAlive()) {
            System.out.println("worker 仍卡在循环里！（未加 volatile 时可能发生）");
            System.exit(1);
        }
        System.out.println("小结：跨线程共享变量要用 volatile / 锁 / 原子类保证可见性");
    }
}
