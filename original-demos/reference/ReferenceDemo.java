package learn.jvm.reference;

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;

/**
 * 四种引用强度演示：强 > 软 > 弱 > 虚。
 *
 * 运行：
 *   java -cp target/classes -Xmx16m -XX:+PrintGC learn.jvm.reference.ReferenceDemo
 *
 * 观察点：
 *   - SoftReference：内存足够时保留，内存不足时被回收（可做缓存）
 *   - WeakReference：下一次 GC 就回收（Map 的 value 若不想被引用常用它，如 ThreadLocal）
 *   - PhantomReference：对象被回收后进入 ReferenceQueue，配合反向引用做清理（如 Netty 的 Cleaner）
 */
public class ReferenceDemo {

    public static void main(String[] args) throws InterruptedException {
        // 1. 强引用：永不回收，宁可 OOM
        Object strong = new byte[1024 * 1024]; // 1MB
        System.out.println("强引用存在，GC 也不会回收它");

        // 2. 软引用：内存够就留着，不够就回收
        SoftReference<byte[]> soft = new SoftReference<>(new byte[2 * 1024 * 1024]); // 2MB
        System.out.println("软引用 GC 前: " + (soft.get() != null));

        // 3. 弱引用：下次 GC 必回收
        WeakReference<Object> weak = new WeakReference<>(new Object());
        System.out.println("弱引用 GC 前: " + (weak.get() != null));

        // 4. 虚引用：get() 永远返回 null，回收后进入 ReferenceQueue
        ReferenceQueue<Object> queue = new ReferenceQueue<>();
        Object phantomTarget = new Object();
        PhantomReference<Object> phantom = new PhantomReference<>(phantomTarget, queue);
        System.out.println("虚引用 get(): " + phantom.get());

        System.out.println("\n--- 执行 System.gc() ---");
        phantomTarget = null; // 先去掉强引用，否则虚引用对象不会被回收
        strong = null;
        System.gc();
        Thread.sleep(500); // 给 GC 一点时间
        System.out.println("软引用 GC 后: " + (soft.get() != null)
                + "   (堆 16m，再分配大对象会触发回收，这里先观察)");
        System.out.println("弱引用 GC 后: " + (weak.get() != null));
        System.out.println("虚引用入队: " + (queue.poll() != null));

        // 演示软引用在内存不足时被回收
        System.out.println("\n--- 分配 10MB 大对象，挤占软引用 ---");
        try {
            byte[] big = new byte[10 * 1024 * 1024];
            System.out.println("大对象分配成功");
        } catch (OutOfMemoryError e) {
            System.out.println("OutOfMemoryError: " + e.getMessage());
        }
        System.out.println("挤占后软引用: " + (soft.get() != null));
    }
}
