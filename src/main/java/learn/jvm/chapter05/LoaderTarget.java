package learn.jvm.chapter05;

/**
 * 被各种 ClassLoader 加载的"靶子类"，内容不重要，重要的是"谁加载了它"。
 */
public class LoaderTarget {
    static {
        System.out.println("    [LoaderTarget 初始化]（被 <clinit> 时喊一声）");
    }

    public void hello() {
        System.out.println("    LoaderTarget.hello()，我的加载器 = "
                + LoaderTarget.class.getClassLoader());
    }
}
