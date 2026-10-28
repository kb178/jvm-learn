# JVM 学习项目

Java 17 + Maven，覆盖 JVM 入门最常考/最实用的 4 个主题：内存、引用、类加载、GC。

## 编译

```bash
cd jvm-learn
mvn -q compile
```

## 实验清单

| 模块 | 类 | 学什么 | 关键 JVM 参数 |
|---|---|---|---|
| 内存 | `memory.StackOverflowDemo` | 栈帧、StackOverflowError | `-Xss256k` |
| 内存 | `memory.HeapOOMDemo` | 堆结构、OOM、heap dump 分析 | `-Xms20m -Xmx20m -XX:+HeapDumpOnOutOfMemoryError` |
| 引用 | `reference.ReferenceDemo` | 强/软/弱/虚四种引用强度 | `-Xmx16m -verbose:gc` |
| 类加载 | `classload.ClassLoaderDemo` | 初始化时机、双亲委派、自定义加载器 | 无 |
| GC | `gc.AllocationDemo` | TLAB、逃逸分析、volatile 可见性 | `-Xms20m -Xmx20m -Xlog:gc*` |

## 运行示例

```bash
# 栈溢出（调小栈更快看到）
java -cp target/classes -Xss256k learn.jvm.memory.StackOverflowDemo

# 堆 OOM + 自动 dump
java -cp target/classes -Xms20m -Xmx20m \
     -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=target/ \
     learn.jvm.memory.HeapOOMDemo

# 四种引用
java -cp target/classes -Xmx16m -verbose:gc learn.jvm.reference.ReferenceDemo

# 类加载（先编译被加载的目标类）
javac -d target/classes src/main/java/learn/jvm/classload/LoaderTarget.java
java -cp target/classes learn.jvm.classload.ClassLoaderDemo

# GC 日志（G1）
java -cp target/classes -Xms20m -Xmx20m -XX:+UseG1GC -Xlog:gc*:file=target/gc.log \
     learn.jvm.gc.AllocationDemo
```

## 建议学习顺序

1. **StackOverflowDemo** — 理解栈帧、方法调用与局部变量
2. **HeapOOMDemo** — 理解堆、对象分配、为什么静态集合会泄漏；用 VisualVM/MAT 打开 `target/*.hprof` 练定位
3. **ReferenceDemo** — 理解 GC 判活与四种引用的使用场景（缓存、ThreadLocal、Netty 清理）
4. **ClassLoaderDemo** — 理解类的一生：加载→验证→准备→解析→初始化；"不同加载器 = 不同类"
5. **AllocationDemo** — 感受 JIT（逃逸分析）和多线程内存可见性

## 推荐工具

- **VisualVM**（JDK 自带：`jvisualvm`）— 监控堆、线程、采样
- **MAT (Eclipse Memory Analyzer)** — 分析 heap dump，找引用链
- `jps` / `jstat -gc <pid> 1000` / `jmap -heap <pid>` — 命令行三件套

## 学习心法

> 不要背参数表，要能在拿到一份 GC 日志 / heap dump 时，回答两个问题：
> 1. 谁占了内存？（引用链）
> 2. GC 为什么没救回来？（是泄漏还是分配太快？）
