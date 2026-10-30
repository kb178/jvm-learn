package learn.jvm.chapter05;

/**
 * 第五章知识点 1：类的初始化时机 —— 六种"主动使用" vs 被动引用
 *
 * 观察方法：每个类的 static 块里喊一声"XX 初始化"，跑一遍看谁喊了谁没喊。
 * 注意：类初始化是"一次性"的，所以每个案例用一对独立的父子类。
 *
 * 六种主动使用（触发 <clinit>）：
 *   1. new 实例化
 *   2. 访问/修改静态字段（编译期常量除外）
 *   3. 调用静态方法
 *   4. 反射 Class.forName("...")
 *   5. 初始化子类时，父类没初始化过 → 先初始化父类（父先子后！）
 *   6. JDK7+ MethodHandle 解析（了解即可，略）
 *
 * 被动引用（不触发，考试/面试常挖）：
 *   - 通过子类访问父类定义的静态字段 → 只初始化父类
 *   - 创建对象数组 new Child[10] → 一个都不初始化
 *   - 访问编译期常量（static final 且字面量）→ 常量已在常量池里
 *
 * 运行：
 *   javac -encoding UTF-8 -d out/classes src/main/java/learn/jvm/chapter05/*.java
 *   java -cp out/classes learn.jvm.chapter05.ClassInitTimingDemo
 */
public class ClassInitTimingDemo {

    // ===== 案例 1 的父子：new 子类 → 父先子后 =====
    static class P1 {
        static { System.out.println("    [P1 初始化]"); }
    }

    static class C1 extends P1 {
        static { System.out.println("    [C1 初始化]"); }
    }

    // ===== 案例 2 的父子：通过子类访问父类字段（被动引用）=====
    static class P2 {
        static int value = 42;
        static { System.out.println("    [P2 初始化]"); }
    }

    static class C2 extends P2 {
        static { System.out.println("    [C2 初始化]"); }
    }

    // ===== 案例 3 的父子：创建对象数组（被动引用）=====
    static class P3 {
        static { System.out.println("    [P3 初始化]"); }
    }

    static class C3 extends P3 {
        static { System.out.println("    [C3 初始化]"); }
    }

    // ===== 案例 4：编译期常量（被动引用）=====
    static class C4 {
        static final int CONST = 42;   // 字面量赋值 = 编译期常量，直接进常量池
        static { System.out.println("    [C4 初始化]"); }
    }

    // ===== 案例 5：调用静态方法（主动使用）=====
    static class C5 {
        static void hello() { System.out.println("    C5.hello() 执行"); }
        static { System.out.println("    [C5 初始化]"); }
    }

    // ===== 案例 6/7：反射 =====
    static class C6 {
        static { System.out.println("    [C6 初始化]"); }
    }

    static class C7 {
        static { System.out.println("    [C7 初始化]"); }
    }

    public static void main(String[] args) throws Exception {

        System.out.println("=== 案例 1：new C1() → 主动使用，父先子后 ===");
        new C1();
        System.out.println("    预期：P1 先喊，C1 后喊（父类 <clinit> 优先）");

        System.out.println("=== 案例 2：System.out.println(C2.value) → 只初始化 P2 ===");
        System.out.println("    打印 value = " + C2.value);
        System.out.println("    预期：只听到 P2 喊，C2 没喊（value 定义在父类里，子类是被动引用）");

        System.out.println("=== 案例 3：new C3[10] → 谁都不初始化 ===");
        C3[] arr = new C3[10];
        arr[0] = null;   // 数组本身是个对象，但里面的 C3 还没造
        System.out.println("    预期：P3/C3 都没喊（创建数组不是主动使用；arr 本身是 [LC3; 对象）");

        System.out.println("=== 案例 4：C4.CONST → 编译期常量，不触发初始化 ===");
        System.out.println("    打印 CONST = " + C4.CONST);
        System.out.println("    预期：C4 没喊（42 在编译期就进常量池了，运行期根本不碰 C4）");

        System.out.println("=== 案例 5：C5.hello() → 调静态方法，主动使用 ===");
        C5.hello();
        System.out.println("    预期：C5 先喊再执行方法");

        System.out.println("=== 案例 6：Class.forName(\"...C6\") → 反射，主动使用 ===");
        Class.forName("learn.jvm.chapter05.ClassInitTimingDemo$C6");
        System.out.println("    预期：C6 喊了（forName 默认会初始化）");

        System.out.println("=== 案例 7：Class.forName(..., initialize=false, ...) → 只加载不初始化 ===");
        Class.forName("learn.jvm.chapter05.ClassInitTimingDemo$C7", false,
                ClassInitTimingDemo.class.getClassLoader());
        System.out.println("    预期：C7 没喊（forName 的 initialize 参数控制是否跑 <clinit>）");

        System.out.println();
        System.out.println("结论：JVM 不是'看见类名就跑 static'，只认'主动使用'清单；");
        System.out.println("      面试挖坑点 = 案例 2/3/4 这三个'看似用了其实没用'的被动引用。");
    }
}
