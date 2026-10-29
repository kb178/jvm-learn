package learn.jvm.chapter03;

import java.util.ArrayList;
import java.util.List;

/**
 * 第三章知识点 3：对象年龄与晋升（配合 GC 日志观察）
 *
 * 主线流程（背下来）：
 *   对象在 Eden 出生
 *     → 每熬过一次 Young GC 年龄 +1（在 Survivor 两块之间倒手）
 *     → 年龄 ≥ 15（-XX:MaxTenuringThreshold，默认 15）→ 晋升老年代
 *
 * 两个"走后门"直接进老年代的例外：
 *   ① 大对象：超过 -XX:PretenureSizeThreshold 的对象不进新生代，
 *      直接在老年代分配（避免在 Eden/Survivor 之间来回复制大块头）
 *   ② 动态年龄判定：Survivor 里同龄对象总大小超过 Survivor 的一半，
 *      年龄 ≥ 该年龄的对象直接晋升
 *
 * 运行（JDK 8 用 -XX:+PrintGCDetails 看详细 GC 日志）：
 *   javac -encoding UTF-8 -d out/classes src/main/java/learn/jvm/chapter03/AgePromotionDemo.java
 *   java -cp out/classes -Xms32m -Xmx32m -XX:PretenureSizeThreshold=4m -XX:+PrintGCDetails \
 *        learn.jvm.chapter03.AgePromotionDemo
 *
 * 日志怎么读（重点看两行数字）：
 *   [GC (Allocation Failure) [PSYoungGen: 65536K->1024K(76288K)] ...
 *                            ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
 *                            新生代回收前 65536K → 回收后 1024K（垃圾被清了）
 *   ...
 *   ParOldGen: 8192K->16384K(171008K)
 *              ^^^^^^^^^^^^^ 老年代涨了 = 大对象直接进来了
 */
public class AgePromotionDemo {

    public static void main(String[] args) throws Exception {

        // ===== 长寿对象：一直被 list 摸得到（GC Root 链不断）=====
        // 它们每熬过一次 Young GC 年龄 +1，攒够 15 次就会晋升老年代
        List<byte[]> survivors = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            survivors.add(new byte[256 * 1024]);   // 4 个 256KB 的"钉子户"
        }

        // ===== 制造垃圾：每次循环 new 完就丢（局部变量被覆盖）=====
        // 256KB × 100 ≈ 25MB，足够把 Eden 塞满好几次 → 触发多次 Young GC
        // 观察日志：每次 GC 后新生代占用大幅下降（垃圾被清），
        //           而那 4 个"钉子户"始终活着，年龄一路 +1
        System.out.println(">>> 开始制造垃圾，触发 Young GC（看 verbose:gc 日志）");
        for (int round = 1; round <= 5; round++) {
            for (int i = 0; i < 100; i++) {
                byte[] garbage = new byte[256 * 1024];  // 用完即丢 = 垃圾
                garbage[0] = 1;                          // 防止被 JIT 优化掉
            }
            System.out.println("    第 " + round + " 轮垃圾投放完毕");
            Thread.sleep(300);                           // 留出时间看日志
        }

        // ===== 大对象演示：8MB，超过 4MB 阈值 → 不去新生代，直接进老年代 =====
        // 观察日志：这次分配前后 ParOldGen（老年代）的占用直接跳涨 ~8MB
        // 对比：如果不加 -XX:PretenureSizeThreshold=4m，它会先进 Eden 再被清走/晋升
        System.out.println(">>> 分配 8MB 大对象（阈值 4MB）→ 直接进老年代，看 ParOldGen 跳涨");
        byte[] big = new byte[8 * 1024 * 1024];
        big[0] = 1;   // 防止被 JIT 优化掉

        System.out.println(">>> 结束。对照源码注释读一遍 GC 日志，重点看：");
        System.out.println("    1) PSYoungGen 行：每次回收前后大小（垃圾被清）");
        System.out.println("    2) ParOldGen 行：大对象那次的跳涨（直接进老年代）");
    }
}
