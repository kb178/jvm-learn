package learn.jvm.chapter03;

import java.lang.ref.WeakReference;

/**
 * 第三章知识点 1：可达性分析 —— GC 怎么判断对象"死没死"
 *
 * 规则：从 GC Roots 出发画引用链，走得到的对象活着，走不到的下次 GC 回收。
 *
 *   GC Root (main 栈帧里的 a)
 *       │
 *       ▼
 *      A ──▶ B ──▶ C      （顺着链走得到 → A、B、C 全活着）
 *
 *      D（没人引用）         （走不到 → 回收）
 *
 * 四种 GC Root（背下来）：
 *   ① 虚拟机栈栈帧中引用的对象（局部变量）—— 第二章的 cache 反事实题就是它
 *   ② 静态属性引用的对象
 *   ③ 常量引用的对象
 *   ④ synchronized 持有的对象
 *
 * 为什么不用"引用计数"？—— A 和 B 互相引用（循环引用）但整体没人用，
 * 引用计数认为它们"活着"，可达性分析能正确回收它们。
 *
 * 本实验的观察技巧：用 WeakReference 当"探针"。
 *   弱引用的特点是"下次 GC 必被回收"（见 ReferenceStrengthDemo），
 *   所以把探针 weakly 地指向某个对象后 System.gc()：
 *     探针.get() == null       → 说明目标对象已经死了（没人强引用它）
 *     探针.get() != null       → 说明目标对象还活着（有强引用链摸得到它）
 *   注意探针自己只是弱引用，不会影响目标的存活判断！
 *
 * 运行：
 *   javac -encoding UTF-8 -d out/classes src/main/java/learn/jvm/chapter03/ReachabilityDemo.java
 *   java -cp out/classes learn.jvm.chapter03.ReachabilityDemo
 */
public class ReachabilityDemo {

    /** 节点类：用 next 串成一条引用链 */
    static class Node {
        final String name;
        Node next;
        Node(String name) { this.name = name; }
        public String toString() { return name; }
    }

    /** GC Root ②：静态字段引用的对象 */
    private static Node staticHolder;

    public static void main(String[] args) throws Exception {

        // ===== 场景 1：引用链上的对象全活着（A → B → C）=====
        Node a = new Node("A");           // a 是 main 栈帧的局部变量 → GC Root ①
        a.next = new Node("B");           // A → B
        a.next.next = new Node("C");      // A → B → C
        // 给 C 装个弱引用探针（探针不影响 C 的死活判定）
        WeakReference<Node> cProbe = new WeakReference<>(a.next.next);

        // ===== 场景 2：孤儿对象 D，切断唯一引用后不可达 =====
        Node d = new Node("D");
        WeakReference<Node> dProbe = new WeakReference<>(d);
        d = null;                          // 切断 D 的唯一引用 → D 变成"孤儿"

        // ===== 场景 3：静态字段持有的对象（GC Root ②）=====
        staticHolder = new Node("S");      // 静态字段攥着 S
        WeakReference<Node> sProbe = new WeakReference<>(staticHolder);

        // ===== 让 GC 干活（System.gc() 是"建议"，一般会真的触发一次 Full GC）=====
        System.gc();
        Thread.sleep(200);                 // 等 GC 跑完

        System.out.println("=== System.gc() 之后 ===");
        System.out.println("C 还活着吗？ " + describe(cProbe.get()) + "   ← A→B→C 链活着，走得到");
        System.out.println("D 还活着吗？ " + describe(dProbe.get()) + "   ← 引用被切断，走不到，回收");
        System.out.println("S 还活着吗？ " + describe(sProbe.get()) + "   ← 静态字段是 GC Root，走得到");

        // ===== 场景 3 续：把静态引用也切断，S 变成孤儿 =====
        staticHolder = null;
        System.gc();
        Thread.sleep(200);
        System.out.println("=== staticHolder = null 再 GC 之后 ===");
        System.out.println("S 还活着吗？ " + describe(sProbe.get()) + "   ← 静态引用断了，S 也走不到了");

        System.out.println();
        System.out.println("结论：GC 不看'谁 new 了它'、'它有没有被别的对象指着'，");
        System.out.println("      只看一件事 —— 从 GC Roots 出发还摸得到摸不到。");
        System.out.println("补充：如果 A 和 B 循环引用（A.next=B, B.next=A）但没人用它们，");
        System.out.println("      可达性分析照样回收它们（引用计数法就做不到 → 所以 JVM 不用引用计数）。");
    }

    private static String describe(Node n) {
        return n == null ? "null（已被回收）" : n.toString() + "（仍存活）";
    }
}
