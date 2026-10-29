package learn.jvm.chapter04;

import java.util.ArrayList;
import java.util.List;

/**
 * 第四章知识点 2：Stop The World —— 调优要消灭的"元凶"
 *
 * GC 的某些阶段（初始标记、复制对象、整理……）必须暂停所有业务线程，这叫 STW。
 * 线上表现：
 *   - 接口 P99 偶发毛刺（平时 50ms，突然 500ms）
 *   - RPC 超时、心跳丢失、节点被注册中心踢掉
 *
 * 本实验的"测量"思路：
 *   主线程不停干活，每次分配都记录距上一次过了多久。平时每次只要几微秒；
 *   GC STW 期间整个线程被"冻住"，恢复后一量——冒出一个几十毫秒的大洞，
 *   这个大洞的时长 ≈ GC 停顿时长。
 *
 * 运行：
 *   javac -encoding UTF-8 -d out/classes src/main/java/learn/jvm/chapter04/*.java
 *   java -cp out/classes -Xms256m -Xmx256m -XX:+PrintGCDetails learn.jvm.chapter04.StopTheWorldDemo
 *
 * 对照方法：程序测出的"最大停顿"和日志里最长那条 GC 的耗时（0.0XX secs）应该量级一致。
 */
public class StopTheWorldDemo {

    /** 钉子户：阶段 2 撑老年代用（静态引用 = GC Root 链不断，第三章的知识） */
    private static final List<byte[]> TENURED = new ArrayList<>();

    public static void main(String[] args) {

        // ===== 阶段 1：快速制造垃圾 → 只有 Young GC → 测小停顿 =====
        System.out.println(">>> 阶段 1：快速分配 12GB 垃圾，测 Young GC 引起的停顿");
        long maxPauseNs = 0;
        long last = System.nanoTime();
        for (int i = 0; i < 400_000; i++) {
            byte[] garbage = new byte[32 * 1024];        // 32KB × 40万 ≈ 12GB 分配压力
            garbage[0] = 1;
            long now = System.nanoTime();
            long gap = now - last;                        // 上次分配到这次分配隔了多久
            if (gap > maxPauseNs) {
                maxPauseNs = gap;
            }
            last = now;
        }
        System.out.println("    阶段 1 最大停顿 ≈ " + maxPauseNs / 1_000_000 + " ms（Young GC 量级，通常几 ms）");

        // ===== 阶段 2：撑大老年代 → Full GC → 测大停顿 =====
        System.out.println(">>> 阶段 2：撑大老年代触发 Full GC，对比停顿量级");
        maxPauseNs = 0;
        last = System.nanoTime();
        try {
            for (int i = 0; i < 300; i++) {
                TENURED.add(new byte[1024 * 1024]);      // 1MB 钉子户，把老年代填满
                long now = System.nanoTime();
                long gap = now - last;
                if (gap > maxPauseNs) {
                    maxPauseNs = gap;
                }
                last = now;
            }
        } catch (OutOfMemoryError e) {
            System.out.println("    （OOM 前 JVM 已反复 Full GC，那些长停顿都被记录下来了）");
        }
        System.out.println("    阶段 2 最大停顿 ≈ " + maxPauseNs / 1_000_000 + " ms（Full GC 量级，大一个数量级）");

        System.out.println(">>> 结论：");
        System.out.println("    1) STW = 业务线程被冻住的时间，测出来的'大洞'就是它；");
        System.out.println("    2) 接口毛刺的常见元凶就是这些洞（毛刺时间点 = GC 停顿时间点）；");
        System.out.println("    3) 调优本质：让 STW 越短越好、越少越好——少 Full GC、控制分配速率。");
    }
}
