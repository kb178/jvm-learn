package learn.jvm.memory;

import java.util.ArrayList;
import java.util.List;

/**
 * 堆内存溢出演示：不断创建对象并被静态集合持有，最终 OutOfMemoryError: Java heap space。
 *
 * 运行（务必限制小堆，否则可能等很久）：
 *   java -cp target/classes -Xms20m -Xmx20m -XX:+HeapDumpOnOutOfMemoryError \
 *        -XX:HeapDumpPath=target/ \
 *        learn.jvm.memory.HeapOOMDemo
 *
 * 观察点：
 *   1. 控制台打印 java.lang.OutOfMemoryError: Java heap space
 *   2. target/ 下生成 *.hprof 文件，用 VisualVM / MAT 打开，
 *      按引用链找到 static List oomList 持有的对象。
 */
public class HeapOOMDemo {

    static class OomObject {
        byte[] filler = new byte[1024]; // 每个对象占 1KB，方便快速填满堆
    }

    // 泄漏点：静态集合一直持有引用，GC 回收不掉
    private static final List<OomObject> oomList = new ArrayList<>();

    public static void main(String[] args) {
        try {
            for (int i = 0; ; i++) {
                oomList.add(new OomObject());
                if (i % 1000 == 0) {
                    System.out.println("已分配 " + i + " 个对象");
                }
            }
        } catch (OutOfMemoryError e) {
            System.out.println("OutOfMemoryError: " + e.getMessage());
            System.out.println("思考：如果把 oomList 改成方法内局部变量，还会 OOM 吗？为什么？");
            throw e;
        }
    }
}
