package learn.jvm.chapter05;

/**
 * 第五章知识点 2：准备阶段的"零值"陷阱 + <clinit> 执行顺序
 *
 * 一、零值陷阱：
 *   static int x = 123; 的一生——
 *     准备阶段：JVM 只分配内存并赋【零值】（int→0, long→0L, boolean→false, 引用→null）
 *     初始化阶段：<clinit> 才执行你写的 123
 *   证据：javap -c 反编译看 <clinit>，123 是在里面"赋"出来的：
 *     javap -c -p -encoding UTF-8 out/classes/learn/jvm/chapter05/ClinitOrderDemo.class
 *
 * 二、<clinit> 顺序规则（3 条）：
 *   1. 按源码文本顺序执行：static 块和 static 字段赋值交替进行，谁在前谁先跑
 *   2. 父类 <clinit> 先于子类（案例 1 里见过）
 *   3. <clinit> 不是必须的：没有 static 成员就没有它
 *
 * 运行：
 *   javac -encoding UTF-8 -d out/classes src/main/java/learn/jvm/chapter05/ClinitOrderDemo.java
 *   java -cp out/classes learn.jvm.chapter05.ClinitOrderDemo
 */
public class ClinitOrderDemo {

    // ===== 案例 1：静态块 vs 字段赋值，比谁写在前面 =====
    static class Order1 {
        static {
            x = 2;                                    // 赋值合法（提前赋值允许）
            // 注意：这里必须用限定名 Order1.x，写简单名 x 会编译报错"非法前向引用"
            //（编译器禁止在声明前用简单名"读取"字段，但限定名/方法调用可以绕过——规则本身也是面试题）
            System.out.println("    static 块执行，把 x 赋成 " + Order1.x);
        }
        static int x = 1;                             // 这行文本在后 → 后执行 → 把 x 覆盖成 1
    }

    // ===== 案例 2：在 static 块里读"还没轮到赋值"的字段 → 看到零值 =====
    static class Order2 {
        static {
            printX();          // 此刻 x 的赋值（=123）还没轮到 → 打印的是准备阶段的零值 0
        }
        static int x = 123;

        static void printX() {
            System.out.println("    static 块里读到 x = " + x + "   ← 零值！赋值还没执行");
        }
    }

    public static void main(String[] args) {

        System.out.println("=== 案例 1：<clinit> 按文本顺序执行 ===");
        System.out.println("    Order1.x 最终值 = " + Order1.x);
        System.out.println("    预期：块先跑（x=2），字段赋值后跑（x=1）→ 最终是 1");

        System.out.println("=== 案例 2：零值陷阱 ===");
        new Order2();   // 触发 Order2 初始化（new = 主动使用）
        System.out.println("    初始化后再读 Order2.x = " + Order2.x + "   ← 现在是 123");
        System.out.println("    预期：static 块里读到 0，<clinit> 跑完后才是 123");

        System.out.println();
        System.out.println("结论：static 字段的值不是'声明就有'，而是 <clinit> 里按文本顺序一笔笔赋出来的；");
        System.out.println("      在此之前它一直是准备阶段的零值——这就是'零值陷阱'。");
        System.out.println("验证命令（看 123 藏在 <clinit> 里）：");
        System.out.println("    javap -c -p out/classes/learn/jvm/chapter05/ClinitOrderDemo.class");
    }
}
