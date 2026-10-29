package learn.jvm.chapter02;

/**
 * 第二章知识点 1：运行时数据区五大区域总览
 *
 * ┌──────────────────────────────────────────────────────────┐
 * │ 区域           │ 线程共享? │ 存什么              │ 会 OOM 吗      │
 * │ 堆             │ 共享      │ 对象实例、数组      │ OOM: heap space│
 * │ 元空间         │ 共享      │ 类元数据、常量池    │ OOM: Metaspace │
 * │ 虚拟机栈       │ 每线程一个│ 栈帧               │ StackOverflow  │
 * │ 本地方法栈     │ 每线程一个│ native 方法调用     │ StackOverflow  │
 * │ 程序计数器     │ 每线程一个│ 当前字节码行号      │ 唯一不会 OOM   │
 * └──────────────────────────────────────────────────────────┘
 *
 * 口诀：堆和元空间共享、可 OOM；栈每线程一份、栈溢出比 OOM 更常见。
 *
 * 运行：
 *   javac -encoding UTF-8 -d out/classes src/main/java/learn/jvm/chapter02/RuntimeDataAreaDemo.java
 *   java -cp out/classes learn.jvm.chapter02.RuntimeDataAreaDemo
 */
public class RuntimeDataAreaDemo {

    // ========== 元空间（Metaspace，概念上属于"方法区"） ==========
    // 这个类被加载时，类名、字段表、方法表、常量池等"类的元信息"就登记在元空间里。
    // 注意：元空间装的是"类的信息"，不是对象！对象在堆里。
    private static int staticCount = 0;   // 静态变量跟着类走（面试概念上算方法区；HotSpot 实现里其实放在堆上与类对象关联，了解即可）
    private static final String GREETING = "hello";  // 字面量"hello"在字符串常量池里（也属元空间概念范畴）

    // ========== 堆（Heap） ==========
    // new 出来的对象都住在堆里，所有线程共享这一块。
    private int instanceField = 42;       // 实例字段：跟着对象走，对象在堆里，它也在堆里
    private byte[] buffer = new byte[1024]; // 数组也是对象，同样在堆里

    public static void main(String[] args) {

        // ========== 虚拟机栈（每个线程一个） ==========
        // 下面这些都是 main 方法栈帧里的"局部变量表"条目：
        RuntimeDataAreaDemo demo = new RuntimeDataAreaDemo(); // demo 本身在堆里，但指向它的引用在栈上
        int localValue = 100;                                  // 基本类型直接存栈帧里
        long bigNumber = 1234567890L;

        // ========== 堆 ==========
        // "new RuntimeDataAreaDemo()" 真正的对象内存分配发生在堆上：
        //   对象头 + instanceField(42) + buffer(1KB) 都在堆里
        // 栈帧里只存了一个"引用"（可以理解为门牌号），指向堆里的对象。

        // ========== 程序计数器（每线程一个） ==========
        // CPU 不停切换线程执行，全靠每个线程的程序计数器记住"我执行到哪一行字节码了"。
        // 它是唯一不会 OOM 的区域——因为它只存一个行号，大小固定。

        // ========== 本地方法栈（每线程一个） ==========
        // 调用 native 方法（C/C++ 实现）时用到，比如 Object.hashCode() 的底层实现：
        int hash = demo.hashCode();   // 这一行背后就经过了本地方法栈

        staticCount++;                // 操作静态变量 → 用的是元空间里登记的类信息
        System.out.println("=== 各区域演示完毕，对照源码注释看每一行属于哪个区域 ===");
        System.out.println("localValue(栈)=" + localValue
                + ", instanceField(堆)=" + demo.instanceField
                + ", staticCount(元空间概念)=" + staticCount
                + ", GREETING(常量池)=" + GREETING
                + ", hashcode(本地方法)=" + hash
                + ", bigNumber(栈)=" + bigNumber);

        // 自测：
        // 1) 如果把 demo 改成方法里的匿名对象用完就丢，它会被哪个区域回收？谁来回收？（答：堆；GC）
        // 2) 递归调用会消耗哪个区域？（答：虚拟机栈）—— 见 StackFrameDemo
    }
}
