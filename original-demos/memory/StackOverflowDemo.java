package learn.jvm.memory;

/**
 * 栈溢出演示：递归过深导致 StackOverflowError。
 *
 * 运行：
 *   java -cp target/classes learn.jvm.memory.StackOverflowDemo
 *   java -cp target/classes -Xss256k learn.jvm.memory.StackOverflowDemo   # 调小栈，更快溢出
 */
public class StackOverflowDemo {

    private int depth = 0;

    public void recurse() {
        depth++;
        recurse();
    }

    public static void main(String[] args) {
        StackOverflowDemo demo = new StackOverflowDemo();
        try {
            demo.recurse();
        } catch (StackOverflowError e) {
            System.out.println("栈深度（递归次数）≈ " + demo.depth);
            System.out.println("StackOverflowError: 线程栈空间耗尽");
            System.out.println("思考：每个方法调用压入一个栈帧，栈帧里有什么？"
                    + "（局部变量表、操作数栈、方法返回地址等）");
        }
    }
}
