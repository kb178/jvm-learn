package learn.jvm.chapter02;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 第二章知识点 4：元空间（Metaspace）
 *
 * 背景：
 *   JDK 8 之前叫"永久代（PermGen）"，放在堆里，大小靠 -XX:MaxPermSize 调（经常不够用）。
 *   JDK 8+ 把它挪到本地内存（操作系统内存），改名"元空间"，默认几乎不限大小。
 *
 * 元空间装什么？——"类的元信息"，不是对象！
 *   类名、字段表、方法表、常量池、方法字节码…… 一个类加载进来就登记一份。
 *
 * 什么时候元空间会 OOM？——加载的类太多：
 *   动态代理（CGLIB/JDK Proxy 每个代理类都是一份新类）、Groovy/脚本引擎、
 *   热部署（每次部署用新 ClassLoader，旧类卸载不掉）……
 *
 * 本实验的思路：
 *   同一份 .class 字节码，交给 N 个"不同的自定义 ClassLoader"去加载。
 *   同名类被不同 loader 加载 → 在 JVM 眼里是 N 个不同的类 → 元空间登记 N 份 → 撑爆。
 *   （这也是第五章"不同 loader = 不同类"的伏笔）
 *
 * 运行（把元空间上限压到 16MB，很快 OOM）：
 *   javac -encoding UTF-8 -d out/classes src/main/java/learn/jvm/chapter02/*.java
 *   java -cp out/classes -XX:MaxMetaspaceSize=16m learn.jvm.chapter02.MetaspaceDemo
 *
 * 观察点：抛 OutOfMemoryError: Metaspace（注意和 "Java heap space" 区分开！）
 */
public class MetaspaceDemo {

    /** 故意把 loader 都攒着不放：loader 被回收 → 它加载的类才能卸载。攒着 = 类永远卸载不掉 */
    private static final List<ClassLoader> KEEP_LOADERS = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        // 读一份现成的 .class 字节码（借用 RuntimeDataAreaDemo 的，内容不重要，重要的是"每份字节码 = 一个新类"）
        byte[] classBytes = readClassBytes("/learn/jvm/chapter02/RuntimeDataAreaDemo.class");

        System.out.println("开始用不同的 ClassLoader 重复加载同一个类……");
        try {
            for (int i = 0; ; i++) {
                // 每次 new 一个新的 loader：同一个类名也会被当成"新类"登记进元空间。
                // 注意父加载器必须传 null！如果传了应用类加载器，loadClass 会先委派父加载器
                // （双亲委派，第五章细讲），而父加载器在 classpath 上就能找到这个类 → 每次
                // 返回的都是同一个类，findClass 根本不会被调用，元空间一点都不会涨。
                ClassLoader loader = new ClassLoader(null) {
                    @Override
                    protected Class<?> findClass(String name) throws ClassNotFoundException {
                        return defineClass(name, classBytes, 0, classBytes.length); // 直接定义类
                    }
                };
                Class<?> clazz = loader.loadClass("learn.jvm.chapter02.RuntimeDataAreaDemo");
                KEEP_LOADERS.add(loader);   // 关键：攥住 loader，防止类被卸载

                if (i % 500 == 0) {
                    System.out.println("已加载 " + (i + 1) + " 份（类名相同，但 loader 不同 → 元空间里是不同的类）");
                }
                // clazz 不用赋值给强变量也没关系——loader 被 KEEP_LOADERS 摁住了，类就卸载不掉
            }
        } catch (OutOfMemoryError e) {
            System.out.println(">>> " + e);   // 预期输出：java.lang.OutOfMemoryError: Metaspace
            System.out.println("对比：堆 OOM 报 'Java heap space'，元空间 OOM 报 'Metaspace'，排查方向完全不同！");
        }
    }

    /** 把 classpath 上的 .class 文件读成字节数组 */
    private static byte[] readClassBytes(String resource) throws IOException {
        try (InputStream in = MetaspaceDemo.class.getResourceAsStream(resource);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        }
    }
}
