package learn.jvm.classload;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 类加载演示：初始化时机 + 自定义类加载器打破双亲委派（同名类被加载两次）。
 *
 * 运行：
 *   java -cp target/classes learn.jvm.classload.ClassLoaderDemo
 *
 * 观察点：
 *   1. 只有真正"主动使用"（new、访问静态字段、反射等）才触发初始化
 *   2. 同一个类被不同 ClassLoader 加载，是两个不同的 Class（互相不可见）
 *   3. ClassLoader 源码中的 loadClass 默认双亲委派；findClass 是留给子类的扩展点
 */
public class ClassLoaderDemo {

    /** 被动引用：通过子类访问父类静态字段，不会触发子类初始化 */
    static class Parent {
        static { System.out.println("Parent 初始化"); }
        static int value = 1;
    }

    static class Child extends Parent {
        static { System.out.println("Child 初始化"); }
    }

    /** 自定义类加载器：从指定目录读 .class，不走双亲委派 */
    static class MyClassLoader extends ClassLoader {
        private final String dir;

        MyClassLoader(String dir) { this.dir = dir; }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            Path path = Path.of(dir, name.replace('.', '/') + ".class");
            try {
                byte[] bytes = Files.readAllBytes(path);
                return defineClass(name, bytes, 0, bytes.length);
            } catch (IOException e) {
                throw new ClassNotFoundException(name, e);
            }
        }

        // 覆盖 loadClass 打破双亲委派：优先自己加载
        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            if (name.startsWith("learn.jvm.classload.LoaderTarget")) {
                Class<?> c = findLoadedClass(name);
                if (c == null) c = findClass(name);
                return c;
            }
            return super.loadClass(name);
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== 1. 初始化时机 ===");
        System.out.println("访问 Child.value = " + Child.value
                + "  → 只触发 Parent 初始化，不触发 Child");

        System.out.println("\n=== 2. 当前类的加载器链 ===");
        ClassLoader cl = ClassLoaderDemo.class.getClassLoader();
        while (cl != null) {
            System.out.println("  " + cl);
            cl = cl.getParent();
        }
        System.out.println("  null  ← 启动类加载器 (Bootstrap)");

        System.out.println("\n=== 3. 自定义加载器加载同一个类 ===");
        // 需要先编译：javac -d target/classes src/main/java/learn/jvm/classload/LoaderTarget.java
        MyClassLoader loader1 = new MyClassLoader("target/classes");
        MyClassLoader loader2 = new MyClassLoader("target/classes");
        Class<?> c1 = loader1.loadClass("learn.jvm.classload.LoaderTarget");
        Class<?> c2 = loader2.loadClass("learn.jvm.classload.LoaderTarget");
        System.out.println("c1 == c2 ? " + (c1 == c2) + "   (应为 false：不同加载器 = 不同类)");
        System.out.println("c1 loader: " + c1.getClassLoader());
        System.out.println("系统 loader 能否直接加载? "
                + (Class.forName("learn.jvm.classload.LoaderTarget") != null));
    }
}
