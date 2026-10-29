package learn.jvm.chapter04;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;

/**
 * 第四章知识点 3：收集器演化 —— 同一个活儿，三个收集器各干一遍
 *
 * 演化主线：让 STW 越来越短
 *   Serial（单线程,停顿长）→ Parallel（多线程,吞吐优先,JDK8 默认）
 *   → CMS（并发,低停顿,有碎片,JDK9 废弃）→ G1（分区,可预测停顿,JDK9+ 默认）
 *   → ZGC / Shenandoah（亚毫秒）
 *
 * 本实验的对比方法：同一段分配压力代码跑三遍，只换 GC 参数，
 * 用 GarbageCollectorMXBeans（JMX 标准接口）统计 GC 次数和总耗时。
 *
 * 运行（三遍只换一个参数）：
 *   javac -encoding UTF-8 -d out/classes src/main/java/learn/jvm/chapter04/*.java
 *   java -cp out/classes -Xms256m -Xmx256m -XX:+UseSerialGC   learn.jvm.chapter04.CollectorsCompareDemo
 *   java -cp out/classes -Xms256m -Xmx256m -XX:+UseParallelGC learn.jvm.chapter04.CollectorsCompareDemo
 *   java -cp out/classes -Xms256m -Xmx256m -XX:+UseG1GC       learn.jvm.chapter04.CollectorsCompareDemo
 *
 * 怎么读结果（三个数字）：
 *   墙钟时间      —— 整个活儿干完用了多久
 *   GC 次数/总耗时 —— 收集器干了多少活、花了多少时间
 *   吞吐量        —— 非 GC 时间占比 = 业务真正干活的时间比例（Parallel 的卖点是这个高）
 * 预期：Serial 最慢、Parallel 吞吐高、G1 停顿更均匀（本实验看总量差异不大，
 *       G1 的优势在"停顿可控"，第 8/9 章用 -XX:MaxGCPauseMillis 感受）。
 */
public class CollectorsCompareDemo {

    public static void main(String[] args) {

        long start = System.nanoTime();

        // ===== 固定工作负载：约 6GB 分配压力（绝大部分是垃圾）=====
        // 三种收集器干的是完全一样的活，公平对比
        for (int i = 0; i < 200_000; i++) {
            byte[] garbage = new byte[32 * 1024];   // 32KB × 20万 ≈ 6GB
            garbage[0] = 1;                         // 防止被 JIT 优化掉
        }

        long wallMs = (System.nanoTime() - start) / 1_000_000;

        // ===== 通过 JMX 标准接口读取 GC 统计 =====
        // 每种收集器注册自己的 MXBean：Serial/Parallel 是 2 个（young+old），
        // G1 可能是 2 个（G1 Young/Old Generation）
        long gcCount = 0, gcMs = 0;
        StringBuilder names = new StringBuilder();
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            if (names.length() > 0) names.append(" + ");
            names.append(gc.getName());
            if (gc.getCollectionCount() > 0) gcCount += gc.getCollectionCount();
            if (gc.getCollectionTime() > 0) gcMs += gc.getCollectionTime();
        }

        System.out.println("收集器： " + names);
        System.out.println("工作负载完成，墙钟时间   " + wallMs + " ms");
        System.out.println("GC 次数 " + gcCount + " 次，GC 总耗时 " + gcMs + " ms");
        System.out.printf("吞吐量 ≈ %.1f%%（非 GC 时间占比，越高说明业务 CPU 越充足）%n",
                100.0 * (wallMs - gcMs) / wallMs);
        System.out.println(">>> 换一种收集器再跑一遍，对比这三个数字！");
    }
}
