package learn.jvm.chapter02;

/**
 * 第二章知识点 2：虚拟机栈与栈帧（重点实验）
 *
 * 核心模型：
 *   每个线程创建时分配一个"虚拟机栈"，每调用一个方法就压入一个"栈帧"，方法返回就弹出。
 *
 *   栈顶 → ┌─────────────┐
 *          │  栈帧 C      │ ← 局部变量表: a=1, b=2（参数和局部变量住这）
 *          ├─────────────┤    操作数栈:   算 a+b 这类临时计算用的中转区
 *          │  栈帧 B      │    动态链接:   指向元空间里该方法的符号引用
 *          ├─────────────┤    返回地址:   方法执行完回到调用者哪一行
 *          │  栈帧 A      │
 *          └─────────────┘
 *
 * 无限递归为什么爆栈？
 *   每递归一层就新压一个栈帧（局部变量、参数至少要占一点空间），
 *   永不 return 就永不弹出，栈空间（-Xss 控制）总有撑爆的一刻 → StackOverflowError。
 *
 * 运行（对比两种栈大小的递归深度）：
 *   java -cp out/classes learn.jvm.chapter02.StackFrameDemo
 *   java -cp out/classes -Xss256k learn.jvm.chapter02.StackFrameDemo
 *
 * 观察点：栈缩小约 4 倍，递归深度也缩小好几倍 → 证明每次调用固定消耗一块栈空间。
 */
public class StackFrameDemo {

    /** 当前递归深度，同时也是"已经压入了多少个栈帧"的计数 */
    private int depth = 0;

    /**
     * 无限递归：每次调用都压入一个新栈帧。
     *   depth++ 用到本栈帧的局部变量表
     *   recurse() 让 JVM 再压一个新栈帧（栈空间再少一格）
     * 永不 return → 栈帧只进不出 → 直到 StackOverflowError。
     */
    public void recurse() {
        depth++;
        recurse();      // 注意：这行之后的代码永远执行不到，因为上面永远不会正常返回
    }

    public static void main(String[] args) {
        StackFrameDemo demo = new StackFrameDemo();
        // 调用 recurse() 时压入第 1 层栈帧（main 自己的栈帧早就已经在栈底了）
        try {
            demo.recurse();
        } catch (StackOverflowError e) {
            // StackOverflowError 是 Error 不是 Exception —— 表示 JVM 资源耗尽，
            // 比 Exception 更严重。真实业务代码不要吞掉它，这里捕获只是为了打印结果。
            System.out.println("递归深度（≈ 栈帧个数）= " + demo.depth);
            System.out.println("栈大小可用 -Xss 调整，试试 -Xss256k 对比深度！");
        }
    }
}
