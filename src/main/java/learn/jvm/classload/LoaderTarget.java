package learn.jvm.classload;

/** 供自定义 ClassLoader 加载的演示类（可单独编译后被加载两次）。 */
public class LoaderTarget {
    static { System.out.println("LoaderTarget 被 " + LoaderTarget.class.getClassLoader() + " 初始化"); }

    public static String hello() { return "hello from LoaderTarget"; }
}
