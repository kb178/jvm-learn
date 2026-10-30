package learn.jvm.chapter05;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 第五章知识点 3：双亲委派模型（含"不同 loader = 不同类"实验）
 *
 * 委派过程：收到加载请求 → 先请父 loader 加载 → 父加载不了才自己 findClass。
 *
 *   Bootstrap（java.lang.*）         ← 请求先一路委派到顶
 *       ↑
 *   Platform（jdk.internal.*）
 *       ↑
 *   Application（你的 classpath）
 *       ↑
 *   自定义 ClassLoader
 *
 * 为什么要有双亲委派（安全）：
 *   保证 java.lang.String 永远是 Bootstrap 加载的真货——你自己造一个
 *   "java.lang.String" 也混不进来（案例 3 演示 JVM 直接拒绝）。
 *
 * 三个案例：
 *   1. 正常委派：自定义 loader 请父帮忙 → 得到 classpath 上的同一个类（== true）
 *   2. 绕过委派：两个 loader 各自 defineClass → 同名类是两个不同的类（== false）
 *   3. 伪造 java.lang.String → JVM 抛 SecurityException: Prohibited package name
 *
 * 回忆杀：MetaspaceDemo 第一版的 bug 就是"委派给了父 loader，defineClass 没执行"——
 *   本案例 1 正是那个坑的"正确演示版"。
 *
 * 运行：
 *   javac -encoding UTF-8 -d out/classes src/main/java/learn/jvm/chapter05/*.java
 *   java -cp out/classes learn.jvm.chapter05.ParentDelegationDemo
 */
public class ParentDelegationDemo {

    public static void main(String[] args) throws Exception {

        // classpath 上的 LoaderTarget（Application loader 加载的"真货"）
        Class<?> appClass = LoaderTarget.class;

        // ===== 案例 1：正常双亲委派 =====
        // 这个 loader 没有重写 loadClass → 默认先委派父 loader（Application）
        // 父 loader 在 classpath 上找到了 LoaderTarget → 直接返回真货
        // 它自己的 findClass 根本不会被调用！
        ClassLoader delegating = new ClassLoader(ParentDelegationDemo.class.getClassLoader()) {
            @Override
            protected Class<?> findClass(String name) throws ClassNotFoundException {
                byte[] bytes = readClassBytes(name);
                return defineClass(name, bytes, 0, bytes.length);
            }
        };
        Class<?> delegated = delegating.loadClass("learn.jvm.chapter05.LoaderTarget");
        System.out.println("=== 案例 1：正常双亲委派 ===");
        System.out.println("    自定义 loader 加载的 == classpath 真货？ " + (delegated == appClass)
                + "   ← true：父 loader 加载的，同一个类");

        // ===== 案例 2：绕过委派，两个 loader 各自定义 =====
        // IsolatedLoader 重写 loadClass 不问父、自己 defineClass
        // → 同一个字节码被定义成两个"不同的类"
        Class<?> c1 = new IsolatedLoader().loadClass("learn.jvm.chapter05.LoaderTarget");
        Class<?> c2 = new IsolatedLoader().loadClass("learn.jvm.chapter05.LoaderTarget");
        System.out.println("=== 案例 2：不同 loader = 不同类 ===");
        System.out.println("    c1 == c2？ " + (c1 == c2) + "   ← false：哪怕字节码一模一样");
        System.out.println("    c1 == classpath 真货？ " + (c1 == appClass) + "   ← false");
        System.out.println("    c1 的加载器 = " + c1.getClassLoader() + "，真货的加载器 = " + appClass.getClassLoader());

        // ===== 案例 3：伪造 java.lang.String =====
        System.out.println("=== 案例 3：伪造核心类，被 JVM 拒绝 ===");
        try {
            // 拿 LoaderTarget 的字节码冒名 defineClass 成 "java.lang.String"——名字一碰 java.* 就被拒
            new IsolatedLoader().defineAs("java.lang.String");
            System.out.println("    竟然成功了？不可能！");
        } catch (Throwable t) {
            System.out.println("    被拒：" + t);
            System.out.println("    ← SecurityException: Prohibited package name——java.* 包只能由 Bootstrap 定义");
        }

        System.out.println();
        System.out.println("结论：1) 委派链保证核心类不可伪造（安全）；");
        System.out.println("      2) '类的唯一性 = 全限定名 + 加载器'，loader 不同就是不同的类；");
        System.out.println("      3) 打破双亲委派的场景：SPI（JDBC DriverManager 回调加载驱动）、");
        System.out.println("         Tomcat 每个 WebApp 独立 loader（应用隔离）、热部署/OSGi。");
    }

    /** 绕过双亲委派的加载器：不问父，自己 defineClass */
    static class IsolatedLoader extends ClassLoader {
        IsolatedLoader() {
            super(null);   // 父 = null（委派给最顶层的 Bootstrap）
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            if (name.startsWith("java.")) {
                // 任何类都要加载 java.lang.Object（父类）——核心类必须走委派交给 Bootstrap。
                // 连"打破委派"的 loader 也不敢碰 java.*，这就是双亲委派存在的意义：
                // 如果谁都能自己定义 java.lang.Object，整个类型体系就崩了。
                return super.loadClass(name);
            }
            return findClass(name);   // ★ 其余类绕过委派：不请示父 loader，直接自己定义
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            byte[] bytes = readClassBytes(name);
            return defineClass(name, bytes, 0, bytes.length);
            // 若 name 是 "java.lang.String"，defineClass 在这里被 JVM 拦下：
            // SecurityException: Prohibited package name: java.lang
        }

        /** 把 LoaderTarget 的字节码冒名定义成任意类名（演示伪造核心类被拒） */
        Class<?> defineAs(String name) {
            byte[] bytes = readClassBytes("learn.jvm.chapter05.LoaderTarget");
            return defineClass(name, bytes, 0, bytes.length);
        }
    }

    /** 把 classpath 上的 .class 读成字节数组（把 learn/jvm/chapter05/LoaderTarget 转成资源路径） */
    static byte[] readClassBytes(String className) {
        String resource = "/" + className.replace('.', '/') + ".class";
        try (InputStream in = ParentDelegationDemo.class.getResourceAsStream(resource);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        } catch (IOException | NullPointerException e) {
            throw new RuntimeException("读不到字节码: " + resource, e);
        }
    }
}
