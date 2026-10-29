package learn.jvm.chapter02;

import java.util.ArrayList;
import java.util.List;

/**
 * 第二章知识点 3：堆 OOM 与 GC Roots（本章最重要的思考题）
 *
 * 问题：为什么 static List 只加不删会 OOM，而方法里的局部变量 List 就不会？
 *
 * 答案钥匙 —— "可达性分析"（第三章细讲，这里先建立直觉）：
 *   GC 判断对象死活不是看"有没有人引用"，而是从 GC Roots 出发画引用链：
 *
 *   情况 A（static List）：                           情况 B（局部 List）：
 *   GC Root: 类的静态属性                              方法返回后，栈帧弹出
 *       │                                                 │
 *       ▼                                                 ✖ 引用消失
 *   static list ──▶ 对象1 对象2 对象3 ...                对象们变成"不可达"
 *       （全都能顺着链摸到 → 全活着 → 谁也回收不掉 → 堆满 OOM）   （GC 直接回收 → 不会 OOM）
 *
 * 运行（20MB 小堆，先跑 B 再跑 A）：
 *   javac -encoding UTF-8 -d out/classes src/main/java/learn/jvm/chapter02/HeapOOMCompare.java
 *   java -cp out/classes -Xmx20m learn.jvm.chapter02.HeapOOMCompare local
 *   java -cp out/classes -Xmx20m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=out/ \
 *        learn.jvm.chapter02.HeapOOMCompare static
 *
 * 观察点：
 *   local  模式：累计分配 100GB 也不 OOM（GC 一直在回收）
 *   static 模式：分配约 1~2 万个就 OOM，还会生成 target/*.hprof 快照（第 6 章用 MAT 分析它）
 */
public class HeapOOMCompare {

    // ========== 情况 A 的"罪魁祸首"：静态 List ==========
    // 它是 GC Root（静态属性），只要类不卸载，list 就永远可达，
    // list 里的对象也永远可达 —— 这就是典型的"内存泄漏"模式。
    private static final List<byte[]> LEAK_LIST = new ArrayList<>();

    public static void main(String[] args) {
        String mode = args.length > 0 ? args[0] : "local";

        if ("static".equals(mode)) {
            leakMode();      // 情况 A：静态集合只加不删 → OOM
        } else {
            localMode();     // 情况 B：局部变量用完即弃 → GC 能回收，不 OOM
        }
    }

    /** 情况 A：每个 1MB 数组都塞进静态 List，GC 谁也不敢动 → 堆（20MB）很快满 */
    private static void leakMode() {
        System.out.println("模式：static（静态集合持有引用，GC 无法回收）");
        try {
            for (int i = 0; ; i++) {
                LEAK_LIST.add(new byte[1024 * 1024]);   // 1MB 一块，永远被 LEAK_LIST 攥着
                if (i % 5 == 0) {
                    System.out.println("已分配 " + (i + 1) + " MB");
                }
            }
        } catch (OutOfMemoryError e) {
            System.out.println(">>> OutOfMemoryError: Java heap space");
            System.out.println("思考：oomList 如果是方法内局部变量，还会 OOM 吗？（跑 local 模式找答案）");
        }
    }

    /** 情况 B：同样分配 1MB 数组，但只用局部变量临时持有，用完就丢 */
    private static void localMode() {
        System.out.println("模式：local（局部变量，方法栈帧持有，用完即可回收）");
        for (int i = 0; i < 100_000; i++) {
            byte[] temp = new byte[1024 * 1024];   // temp 只在本栈帧的局部变量表里
            temp[0] = 1;                           // 防止被 JIT 优化掉
            // 循环下一轮：上一个数组没有任何引用了（栈帧里被覆盖/方法结束后消失）
            // → 对 GC Roots 不可达 → GC 随时可回收 → 所以 10 万次也不 OOM
            if (i % 10000 == 0) {
                System.out.println("已分配 " + (i + 1) + " MB（GC 一直在后台回收旧数组）");
            }
        }
        System.out.println(">>> 分配 10 万个 1MB 数组（累计 100GB）也没有 OOM！（对比 static 模式）");
        System.out.println("结论：泄漏的本质 = 有东西（GC Root 链）一直引用着，GC 回收不掉。");
    }
}
