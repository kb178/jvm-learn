package learn.jvm.chapter03;

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * 第三章知识点 2：四种引用强度（强 > 软 > 弱 > 虚）
 *
 * ┌──────┬──────────────────────┬────────────────────────┐
 * │ 强引用 │ 永不回收，宁可 OOM     │ 日常代码 99%            │
 * │ 软引用 │ 内存不足时才回收       │ 内存敏感缓存            │
 * │ 弱引用 │ 下次 GC 必回收        │ ThreadLocal、WeakHashMap│
 * │ 虚引用 │ get() 永远 null      │ Netty 堆外内存清理       │
 * │       │ 回收后从队列收通知     │ （配合 ReferenceQueue）  │
 * └──────┴──────────────────────┴────────────────────────┘
 *
 * ReferenceQueue 的作用：对象被回收后，JVM 把对应的引用对象丢进队列，
 * 程序通过 poll() 得知"XX 已经死了"，好去做清理工作（比如释放堆外内存）。
 *
 * 运行（16MB 小堆，方便挤出"内存不足"）：
 *   javac -encoding UTF-8 -d out/classes src/main/java/learn/jvm/chapter03/ReferenceStrengthDemo.java
 *   java -cp out/classes -Xmx16m learn.jvm.chapter03.ReferenceStrengthDemo
 */
public class ReferenceStrengthDemo {

    public static void main(String[] args) throws Exception {

        ReferenceQueue<byte[]> queue = new ReferenceQueue<>();   // 回收通知队列

        // ===== 1. 强引用：最普通的就是它 =====
        byte[] strong = new byte[1024 * 1024];                   // 1MB

        // ===== 2. 软引用：包一层 SoftReference =====
        SoftReference<byte[]> soft = new SoftReference<>(new byte[4 * 1024 * 1024], queue);

        // ===== 3. 弱引用 =====
        WeakReference<byte[]> weak = new WeakReference<>(new byte[1024 * 1024], queue);

        // ===== 4. 虚引用：必须配 ReferenceQueue，get() 永远返回 null =====
        byte[] phantomTarget = new byte[1024 * 1024];
        PhantomReference<byte[]> phantom = new PhantomReference<>(phantomTarget, queue);
        phantomTarget = null;    // 切断强引用，让虚引用独自面对 GC

        System.out.println("=== 刚创建 ===");
        System.out.println("软引用 get(): " + (soft.get() != null ? "有数据" : "null"));
        System.out.println("弱引用 get(): " + (weak.get() != null ? "有数据" : "null"));
        System.out.println("虚引用 get(): " + phantom.get() + "   ← 永远是 null，这是设计如此！");

        // ===== 第一轮 GC：弱引用必死，软引用活着，虚引用对象可能被回收并入队 =====
        System.gc();
        Thread.sleep(200);

        System.out.println();
        System.out.println("=== System.gc()（内存充足）之后 ===");
        System.out.println("强引用对象: " + (strong != null ? "还在" : "没了") + "   ← GC 永远不敢动它");
        System.out.println("软引用 get(): " + (soft.get() != null ? "还在" : "null") + "   ← 内存充足就留着");
        System.out.println("弱引用 get(): " + (weak.get() != null ? "还在" : "null") + "   ← 说死就死");
        Reference<?> deadOne = queue.poll();
        System.out.println("ReferenceQueue 收到通知: " + (deadOne != null ? deadOne.getClass().getSimpleName() : "无"));

        // ===== 第二轮：把内存挤爆，软引用也得死 =====
        System.out.println();
        System.out.println("=== 开始吃内存，挤掉软引用 ===");
        List<byte[]> hog = new ArrayList<>();     // 强引用攒着，制造"内存不足"
        try {
            while (soft.get() != null) {
                hog.add(new byte[1024 * 1024]);
                if (hog.size() % 4 == 0) {
                    System.gc();                  // 帮 GC 一把，加速软引用清理
                }
            }
        } catch (OutOfMemoryError e) {
            // 即使 OOM，JVM 抛出前也会先尝试清软引用
            System.out.println("（触发了 OOM，JVM 抛出前已经清过软引用）");
        }
        System.out.println("软引用 get(): " + (soft.get() != null ? "还在" : "null") + "   ← 内存不足，终于被回收");
        System.out.println("强引用对象: " + (strong != null ? "还在" : "没了") + "   ← 堆快爆了它都活着（宁可 OOM）");

        System.out.println();
        System.out.println("结论：四种引用 = 四种'豁免程度'，给 GC 回收对象时的优先级排了个序。");
        System.out.println("实战：用缓存用软引/WeakHashMap；ThreadLocal 用完 remove（它是弱引用 key）；");
        System.out.println("      虚引用是 Netty 释放堆外内存的信号枪。");
    }
}
