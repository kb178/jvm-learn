package learn.jvm.chapter04;

import java.util.ArrayList;
import java.util.List;

/**
 * 第四章知识点 1：GC 的三种类型与触发条件（学会在日志里"认脸"）
 *
 * | 类型       | 触发条件                          | 停顿        | 线上态度   |
 * | Young GC   | Eden 满                           | 几~几十ms   | 正常现象   |
 * | Major GC   | 老年代满（CMS 特有叫法）           | 较长        | 看情况     |
 * | Full GC    | 老年代满 / 元空间满 / System.gc() / | 百ms~秒级   | 警惕！     |
 * |            | 晋升失败                          |            |           |
 *
 * 口诀：Young GC 频繁正常，Full GC 频繁才是病。
 *
 * 运行：
 *   javac -encoding UTF-8 -d out/classes src/main/java/learn/jvm/chapter04/*.java
 *   java -cp out/classes -Xms256m -Xmx256m -XX:+PrintGCDetails learn.jvm.chapter04.GcTypesDemo
 *
 * 日志里认脸（触发原因写在括号里，这是排查的黄金线索）：
 *   [GC (Allocation Failure) [PSYoungGen: ...]]
 *        ^^^^^^^^^^^^^^^^^^ Eden 满了 → Young GC
 *   [Full GC (Ergonomics) [PSYoungGen: ... ParOldGen: ...]]
 *        ^^^^^^^^^^^^^^^^ JVM 自适应判断老年代吃紧 → Full GC（整个堆都动）
 *   [Full GC (System.gc()) ...]
 *        ^^^^^^^^^^^^^^^^ 代码里显式调用（框架误调 System.gc() 时看得到它）
 */
public class GcTypesDemo {

    /** 老年代钉子户：一直攒着它们，把老年代撑起来 */
    private static final List<byte[]> TENURED = new ArrayList<>();

    public static void main(String[] args) throws Exception {

        // ===== 阶段 1：制造垃圾 → Eden 满 → 触发多次 Young GC =====
        // new 完就丢的数组 = 纯垃圾，塞满 Eden 就触发 (Allocation Failure)
        System.out.println(">>> 阶段 1：投放垃圾，触发 Young GC（日志里数数有几条 'GC (Allocation Failure)'）");
        for (int round = 1; round <= 3; round++) {
            for (int i = 0; i < 200; i++) {
                byte[] garbage = new byte[256 * 1024];   // 256KB × 200 ≈ 50MB 垃圾/轮
                garbage[0] = 1;                           // 防止被 JIT 优化掉
            }
            System.out.println("    第 " + round + " 轮垃圾投放完毕");
        }

        // ===== 阶段 2：往老年代攒钉子户 → 老年代吃紧 → 触发 Full GC =====
        // TENURED 被静态字段攥着（第三章学的：GC Root 链不断），对象死不掉
        // 这些对象逐渐占据老年代；当老年代快满时，JVM 被迫 Full GC（日志看 'Full GC (Ergonomics)'）
        System.out.println(">>> 阶段 2：攒钉子户撑老年代，触发 Full GC（日志里看 'Full GC (Ergonomics)'）");
        try {
            for (int i = 0; i < 200; i++) {
                TENURED.add(new byte[1024 * 1024]);      // 1MB × 200 = 200MB，老年代极限约 170MB
                Thread.sleep(20);                         // 放慢速度，方便对照日志
            }
        } catch (OutOfMemoryError e) {
            // 堆撑爆之前 JVM 会反复 Full GC 垂死挣扎，日志里能看到好几条
            System.out.println("    （撑爆前 JVM 已经反复 Full GC 拯救过了）");
        }

        // ===== 阶段 3：显式 System.gc() → 触发 Full GC (System.gc()) =====
        System.out.println(">>> 阶段 3：调用 System.gc()（日志里看 'Full GC (System.gc())'）");
        System.gc();

        System.out.println(">>> 结束。数一数日志里 Young GC 几条、Full GC 几条？");
        System.out.println("    排障口诀：拿到 gc 日志先看触发原因（括号里的词），再看回收量和耗时。");
    }
}
