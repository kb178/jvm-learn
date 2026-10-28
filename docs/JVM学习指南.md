# JVM 实战学习指南（80% 场景够用版）

> 目标：不是背规范，而是拿到一台线上 Java 服务时，你能回答三个问题——
> 1. 内存被谁占了？
> 2. GC 为什么慢？
> 3. CPU 为什么高？
>
> 配套可运行代码见本仓库 `src/main/java/learn/jvm/`，每章末尾有对应实验。

---

## 🗺️ 完整学习大纲

> 用法：按阶段推进。标 ✅ 的章节下文已有初版正文；标 📝 的是待细化专题，学到时让我展开即可。
> 每个知识点都标了优先级：**P0** 必须扎实 / **P1** 熟悉 / **P2** 了解即可。

### 阶段一：地基（第 1~2 周）

#### 1. JVM 全景 ✅ P0
- [ ] JVM 在 Java 体系中的位置：javac → 字节码 → JVM → 机器码
- [ ] HotSpot 主要子系统：类加载、运行时数据区、执行引擎
- [ ] 常见 JVM 实现对比（HotSpot / OpenJ9 / GraalVM）——P2

#### 2. 运行时数据区 ✅ P0
- [ ] 五大区域职责、线程共享与否、各自会抛什么 Error
- [ ] 堆结构：新生代（Eden/S0/S1）、老年代、比例
- [ ] 栈结构：栈帧 = 局部变量表 + 操作数栈 + 动态链接 + 返回地址
- [ ] 元空间 vs 永久代（JDK 7 vs 8 的变化）
- [ ] 直接内存（Direct Memory）与堆外内存 📝 P1 —— 待细化：`-XX:MaxDirectMemorySize`、Netty 场景

#### 3. 对象的内存布局与分配 ✅ P1
- [ ] 对象头（Mark Word：锁状态/GC 年龄/hashcode）、实例数据、对齐填充
- [ ] 指针压缩 `-XX:+UseCompressedOops`——P1
- [ ] 分配流程：TLAB → Eden → 指针碰撞 / 空闲列表
- [ ] 逃逸分析、标量替换、栈上分配 📝 P1——待细化：配合 `-XX:+PrintEscapeAnalysis` 演示
- [ ] 大对象直接进老年代、动态年龄判定 ✅

#### 4. 引用类型 ✅ P0
- [ ] 强 / 软 / 弱 / 虚四种强度与回收时机
- [ ] `ReferenceQueue` 的作用
- [ ] 实战场景：缓存、`WeakHashMap`、`ThreadLocal`、Netty Cleaner

### 阶段二：GC 核心（第 2~3 周）

#### 5. 垃圾收集算法 📝 P0——待细化：配图解
- [ ] 标记-清除：碎片问题
- [ ] 标记-复制：Eden/Survivor、为什么是 8:1:1
- [ ] 标记-整理：老年代为什么用它
- [ ] 分代收集思想：为什么"分代"是对的

#### 6. 判定对象存活 ✅ P0
- [ ] 可达性分析、GC Roots 有哪几种
- [ ] 四次标记与 finalize()——P2
- [ ] 引用计数法为什么不行（循环引用）——P1

#### 7. 经典收集器 ✅ P1
- [ ] 新生代：Serial / ParNew / Parallel Scavenge
- [ ] 老年代：Serial Old / CMS / Parallel Old
- [ ] CMS 四个阶段（初始标记/并发标记/重新标记/并发清除）📝 P1——待细化：三色标记 + 漏标处理
- [ ] Stop The World 什么时候发生 ✅

#### 8. G1 收集器 ✅ P0
- [ ] Region 划分、Humongous 对象
- [ ] 可预测停顿：`-XX:MaxGCPauseMillis` 怎么工作
- [ ] Young GC / Mixed GC / Full GC 触发条件 📝 P0——待细化
- [ ] RSet / CardTable 记忆集——P1
- [ ] 关键日志逐行解读 📝 P0——待细化：贴真实 gc.log 标注

#### 9. ZGC / Shenandoah 📝 P2——待细化：原理概览
- [ ] 着色指针、读屏障
- [ ] 亚毫秒停顿怎么做到的
- [ ] 适用场景与 JDK 17/21 的演进

#### 10. GC 日志与调优参数 ✅ P0
- [ ] JDK 9+ `-Xlog:gc*` 配置
- [ ] 看懂：GC 触发原因、回收前后大小、耗时
- [ ] jstat 每列含义与判断标准 📝 P0——待细化：贴一张真实输出逐列讲
- [ ] 生产基线参数 ✅

### 阶段三：类加载与字节码（第 3~4 周）

#### 11. 类加载机制 ✅ P0
- [ ] 加载 → 验证 → 准备 → 解析 → 初始化 各阶段做什么
- [ ] 准备阶段的"零值"陷阱
- [ ] 六种主动使用触发初始化
- [ ] `<clinit>` / `<init>` 区别——P1

#### 12. 双亲委派 ✅ P0
- [ ] 三层类加载器与委派过程
- [ ] 为什么要双亲委派（`java.lang.String` 安全）
- [ ] 打破双亲委派的场景：SPI、Tomcat、热部署
- [ ] 线程上下文 ClassLoader——P1

#### 13. 字节码基础 📝 P2——待细化
- [ ] 用 `javap -c` 读一段字节码
- [ ] 常见指令：`new` / `dup` / `invokevirtual` / `invokespecial`
- [ ] 方法调用 5 种指令的区分

### 阶段四：执行引擎与诊断（第 4~6 周）

#### 14. JIT 编译 📝 P1——待细化
- [ ] 解释执行 vs 编译执行、热点代码探测（方法调用计数器 + 回边计数器）
- [ ] 分层编译（C1/C2）
- [ ] 方法内联：为什么小方法快
- [ ] `-XX:+PrintCompilation` 看懂编译日志
- [ ] 逃逸分析与锁消除/锁粗化

#### 15. 诊断工具箱 ✅ P0
- [ ] jps / jstat / jmap / jstack / jcmd ✅
- [ ] VisualVM 实操 📝 P1——待细化：监控页怎么读
- [ ] MAT 分析 dump 完整流程 📝 P0——待细化：Dominator Tree / Path to GC Roots 一步步点
- [ ] Arthas 常用命令 📝 P0——待细化：dashboard / thread / trace / watch / heapdump
- [ ] JFR 飞行记录 📝 P1——待细化：生产低开销采样怎么开

#### 16. 内存泄漏排查 ✅ P0
- [ ] 判断：OU 只涨不跌
- [ ] 导 dump、MAT 定位引用链
- [ ] 常见泄漏模式清单 ✅（静态集合 / 监听器 / 流未关 / ThreadLocal / ClassLoader）
- [ ] 完整实战案例（从报警到修复）📝 P0——待细化：带真实日志和 dump 分析步骤

#### 17. 线上问题排查 📝 P0——待细化：SOP 手册
- [ ] Full GC 频繁排查树 ✅ + 扩展案例
- [ ] CPU 100% 排查（top -Hp + jstack）✅ + 死锁案例 📝——待细化：`Found one Java-level deadlock` 解读
- [ ] 接口毛刺：先查 GC 还是先查业务
- [ ] OOM 应急 SOP ✅ + 各类 OOM 分型（heap / metaspace / direct / unable to create native thread）

### 阶段五：进阶专题（按需学）

#### 18. 堆外内存与 Netty 📝 P1——待细化
- [ ] `DirectByteBuffer` 分配与回收
- [ ] `OutOfMemoryError: Direct buffer memory` 排查
- [ ] ` Cleaner` 与虚引用的关系

#### 19. 容器化 JVM（K8s/Docker）📝 P0——待细化
- [ ] `-XX:+UseContainerSupport`（JDK 10+ 默认开）
- [ ] 容器 limit ≠ `-Xmx` 的坑：RSS 被 OOMKill
- [ ] 推荐：`-XX:MaxRAMPercentage` / `-XX:InitialRAMPercentage`
- [ ] CPU limit 与 `ActiveProcessorCount`

#### 20. 性能优化专题 📝 P1——待细化
- [ ] 对象池的利与弊（为什么 HikariCP 池连接不池对象）
- [ ] 减少分配的具体手法（StringBuilder 复用、批量处理、懒加载）
- [ ] 锁与 synchronized 的偏向/轻量/重量升级——和 JIT 联动
- [ ] 微基准测试陷阱（JMH）——P2

#### 21. 深入 GC 调优案例集 📝 P0——待细化：多写几个真实改编案例
- [ ] 案例：静态集合泄漏 ✅（已有简版）
- [ ] 案例：循环分配导致 YGC 毛刺 ✅（已有简版）
- [ ] 案例：元空间 OOM（动态代理/热部署）
- [ ] 案例：堆外内存泄漏（Netty UnreleasableByteBuf）
- [ ] 案例：K8s 里容器频繁 OOMKill
- [ ] 案例：CMS Concurrent Mode Failure 升级 G1
- [ ] 案例：G1 大堆 Mixed GC 跟不上分配

### 学习方式建议

1. **每个知识点 = 概念 + 你能说出的现象 + 一条验证命令/实验**
2. 学到哪一块，直接对我说：「细化第 X 章 / 把 Y 知识点展开」，我会补正文、加可运行 Demo、配真实日志示例
3. 阶段一、二是地基，别跳；阶段四起可以按工作需要选学

---

## 第一章：JVM 是什么

JVM（Java Virtual Machine）是运行 Java 字节码的虚拟机。Java 的"一次编译，到处运行"就靠它：

```
.java ──javac──> .class（字节码）──JVM──> 机器码
                                      ↑
                          不同平台装不同 JVM 实现
                          （HotSpot / OpenJ9 / GraalVM...）
```

平时说"调 JVM"，调的主要是 **HotSpot**（Oracle/OpenJDK 默认）。它由几大子系统组成，只需要重点关注两块：

```
┌─────────────────────────────────────────────┐
│                  JVM                        │
│  ┌─────────────┐   ┌──────────────────────┐ │
│  │  类加载子系统 │   │   运行时数据区（重点） │ │
│  │ ClassLoader │   │  堆 / 栈 / 元空间 ...  │ │
│  └─────────────┘   └──────────────────────┘ │
│  ┌─────────────┐   ┌──────────────────────┐ │
│  │  执行引擎    │   │  本地方法接口 (JNI)    │ │
│  │ JIT / GC    │   │                      │ │
│  └─────────────┘   └──────────────────────┘ │
└─────────────────────────────────────────────┘
```

**一句话**：类加载器负责把 class 装进内存；运行时数据区是内存布局；GC 负责回收；JIT 把热点代码编译成本地机器码。

---

## 第二章：内存区域（最重要，占学习时间 40%）

### 2.1 运行时数据区一览

| 区域 | 线程共享？ | 存什么 | 会 OOM 吗 | 会溢出吗 |
|---|---|---|---|---|
| **堆** | ✅ 共享 | 对象实例、数组 | ✅ `Java heap space` | — |
| **元空间** | ✅ 共享 | 类元数据、常量池 | ✅ `Metaspace` | — |
| **虚拟机栈** | ❌ 每线程一个 | 栈帧（局部变量表、操作数栈） | ✅ （内存不足时） | ✅ `StackOverflowError` |
| **本地方法栈** | ❌ 每线程一个 | Native 方法（如 `Object.hashCode`） | ✅ | ✅ |
| **程序计数器** | ❌ 每线程一个 | 当前字节码行号 | ❌ 唯一不会 OOM 的区域 | — |

记忆口诀：**堆和元空间共享、可 OOM；栈每线程一份、栈溢出比 OOM 更常见**。

### 2.2 堆——对象的家（重点中的重点）

HotSpot 默认把堆分成两块，配合分代假说：

```
堆 (Heap)
├── 新生代 Young Generation          ← 新对象都在这里出生
│   ├── Eden (8/10)                 ← 对象优先分配在这
│   ├── Survivor S0 (1/10)
│   └── Survivor S1 (1/10)
└── 老年代 Old Generation (2/3)      ← 长期存活的对象晋升到这里
```

**为什么要分代**？IBM 研究发现：98% 的对象朝生夕死。新对象放新生代，GC 只清一小块，快；活下来的反复熬，熬过 15 次 GC 晋升老年代。

**对象分配流程**（背下来，面试和排障都用）：

```
new 对象
  │
  ├─ 1. 优先在当前线程的 TLAB（Thread Local Allocation Buffer）分配，无锁，快
  │
  ├─ 2. TLAB 不够 → Eden 区 CAS + 指针碰撞分配
  │
  ├─ 3. Eden 不够 → 触发 Young GC（Minor GC）
  │     └─ 还不够（对象太大或 GC 后仍不够）→ 提升到老年代，再不够 → Full GC
  │
  └─ 4. 老年代也放不下 → Full GC 还不够 → OutOfMemoryError: Java heap space
```

### 2.3 栈——方法调用的记录

每个线程创建时分配一个虚拟机栈，每个方法调用压入一个**栈帧**：

```
methodA() 调用 methodB() 调用 methodC():

栈顶 → ┌─────────────┐
       │  栈帧 C      │  ← 局部变量表: a=1, b=2
       ├─────────────┤       操作数栈: 临时计算
       │  栈帧 B      │       动态链接: 指向方法区的符号引用
       ├─────────────┤       返回地址: 回到 B 的哪一行
       │  栈帧 A      │
       ├─────────────┤
       │  ...        │
       └─────────────┘
```

**`-Xss` 控制每个线程的栈大小**（默认约 512K~1M，看平台）。递归太深 → `StackOverflowError`。

👉 **动手**：`memory.StackOverflowDemo`，用 `-Xss256k` 跑，观察递归深度变化。

### 2.4 元空间（Metaspace）

JDK 8 之前叫"永久代（PermGen）"，放在堆里；JDK 8+ 挪到**本地内存**，叫元空间。装的是**类的元信息**（类名、字段、方法、常量池），不是对象。

典型 OOM 场景：
- 动态生成大量类（CGLIB、Groovy、反射、热部署）
- 不停地 `ClassLoader` 泄漏（每个 loader 加载的类都卸载不掉）

```bash
-XX:MaxMetaspaceSize=256m   # 上限，超过就 OOM: Metaspace
jcmd <pid> VM.classloader_stats   # 查看各 loader 加载了多少类
```

---

## 第三章：对象的一生

### 3.1 对象怎么判定"死没死"？

**可达性分析**（不是引用计数！）：从 GC Roots 出发，走得到的对象活着，走不到的回收。

GC Roots 包括：
- 虚拟机栈栈帧中引用的对象（局部变量）
- 静态属性引用的对象
- 常量引用的对象
- `synchronized` 持有的对象

```
GC Root (main 栈里的 list)
    │
    ▼
  ArrayList ──> element[0] ──> Object A ──> Object B   （全活着）
                     │
                element[1] ──> Object C                 （活着）
                
Object D（无人引用）→ 不可达 → 下次 GC 回收
```

### 3.2 四种引用强度（缓存、框架源码必考）

| 引用 | 回收时机 | 典型用途 |
|---|---|---|
| **强引用** `Object o = new Object()` | 永不回收，宁可 OOM | 日常代码 99% 都是 |
| **软引用** `SoftReference` | 内存不足时回收 | 内存敏感缓存 |
| **弱引用** `WeakReference` | 下次 GC 必回收 | `ThreadLocal`、`WeakHashMap` |
| **虚引用** `PhantomReference` | 对象回收后收到通知 | Netty 堆外内存清理 |

👉 **动手**：`reference.ReferenceDemo`，观察弱引用在 `System.gc()` 后消失、软引用在内存不足时被挤掉。

### 3.3 对象年龄与晋升

```
对象在 Eden 出生 → 第一次 Minor GC 活下来 → 进 S0，年龄=1
                → 再次 Minor GC 活下来 → S0↔S1 来回倒，年龄+1
                → 年龄 ≥ 15（默认阈值）→ 晋升老年代

例外（直接进老年代）：
  1. 大对象直接进老年代（-XX:PretenureSizeThreshold）
  2. 动态年龄判定：S 区同龄对象总大小超过 S 区一半，年龄大的直接晋升
```

---

## 第四章：垃圾回收（GC）——调优的主战场

### 4.1 GC 的两种类型

| 类型 | 触发条件 | 停顿 | 特点 |
|---|---|---|---|
| **Minor GC / Young GC** | Eden 满 | 短（几~几十 ms） | 频繁但快，正常现象 |
| **Major GC / Old GC** | 老年代满 | 较长 | 看情况 |
| **Full GC** | 老年代满 / 元空间满 / `System.gc()` / 堆分配失败 | **长（几百 ms~秒级）** | **线上要警惕** |

**调优的核心目标之一：减少 Full GC 次数和 Full GC 停顿时间。**

### 4.2 三色标记与常见收集器演化

```
Serial (单线程, 停顿长, JDK8 客户端)
  ↓ 多线程化
Parallel / Parallel Old (吞吐优先, JDK8 默认服务端)  ← -XX:+UseParallelGC
  ↓ 减少停顿
CMS (并发标记清除, 低停顿, 有碎片, JDK9 废弃)
  ↓ 分区 + 可预测停顿
G1 (JDK9+ 默认)                                    ← -XX:+UseG1GC
  ↓ 更极致的低延迟
ZGC (<1ms 停顿, 适合大堆)                          ← -XX:+UseZGC
Shenandoah (低停顿, 社区版 ZGC)
```

**怎么选（背结论）**：

| 场景 | 推荐 |
|---|---|
| 普通 Web 服务 / 微服务（堆 ≤ 几 GB） | **G1（默认）**，基本不用换 |
| 堆很大（几十 GB+）、延迟敏感 | **ZGC** |
| 离线批处理、吞吐优先 | Parallel |
| JDK 8 老项目没特殊需求 | Parallel 够用，有问题再上 G1 |

### 4.3 G1 核心概念（现代 JVM 必懂）

G1 把堆切成上千个等大的 **Region**（不是物理连续的 Eden/Old），每个 Region 动态扮演 Eden / Survivor / Old / Humongous（大对象）：

```
┌──┬──┬──┬──┬──┬──┬──┬──┐
│ E│ E│ S│ O│ H│ O│ E│ O│   E=Eden  S=Survivor
├──┼──┼──┼──┼──┼──┼──┼──┤   O=Old   H=Humongous(大对象)
│ O│ E│ S│ O│ O│ H│ O│ S│
└──┴──┴──┴──┴──┴──┴──┴──┘
```

关键参数（只需要记这两个）：

```bash
-XX:MaxGCPauseMillis=200   # G1 的卖点：尽量把停顿控制在目标内（默认200ms）
-XX:G1HeapRegionSize=16m   # Region 大小，堆越大调越大
```

### 4.4 到底什么是"Stop The World"？

GC 的某个阶段（如初始标记）必须**暂停所有业务线程**。表现就是：

- 接口偶发毛刺（P99 突然 500ms）
- Dubbo/RPC 超时、心跳丢失、节点被踢
- 日志里出现 `Pause Young (Normal) (G1 Evacuation Pause)` 这类字样

调优本质：**让 STW 越短越好、越少越好**。

---

## 第五章：类加载机制

### 5.1 类的一生

```
加载 → 验证 → 准备 → 解析 → 初始化 → 使用 → 卸载
         │       │       │        │
         │       │       │        └─ <clinit>：静态变量赋值 + static 块
         │       │       └─ 符号引用 → 直接引用
         │       └─ 静态变量分配内存、赋零值（int=0，不是=123）
         └─ 字节码格式校验、安全检查
```

**面试常坑**：准备阶段静态变量先赋**零值**，初始化阶段才赋程序员写的值：

```java
public class A {
    static int x = 123;
}
// 准备阶段后：x = 0
// 初始化阶段后：x = 123
```

### 5.2 什么时候触发初始化？

只有 6 种"主动使用"才触发 `<clinit>`：

1. `new` 实例化
2. 访问/修改静态字段（`final` 常量除外）
3. 调用静态方法
4. 反射 `Class.forName(...)`
5. 初始化子类时，父类没初始化过
6. JDK 7+ 的 `java.lang.invoke.MethodHandle` 解析

👉 **动手**：`classload.ClassLoaderDemo` 第 1 节——访问 `Child.value` 只触发 `Parent` 初始化，不触发 `Child`（被动引用）。

### 5.3 双亲委派模型

```
              ┌─────────────────┐
              │ Bootstrap       │  加载 java.lang.*、核心库
              └────────┬────────┘
                       │ 
              ┌────────▼────────┐
              │ Platform        │  加载 jdk.internal.*
              └────────┬────────┘
                       │
              ┌────────▼────────┐
              │ Application     │  加载你 classpath 的类
              └────────┬────────┘
                       │
              ┌────────▼────────┐
              │ 自定义 ClassLoader│
              └─────────────────┘

加载请求先交给父 loader，父 loader 加载不了才自己找 → 保证 java.lang.String 永远是 bootstrap 加载的，不会被伪造
```

**什么时候打破双亲委派？**
- SPI 机制（JDBC：bootstrap 加载 `DriverManager`，需要回调 app loader 加载驱动）→ 线程上下文 ClassLoader
- 热部署 / OSGi / Tomcat 每个 WebApp 一个 loader（应用间类隔离）

👉 **动手**：`classload.ClassLoaderDemo` 第 3 节——同一个类被两个自定义 loader 加载，`c1 == c2` 为 **false**（不同 loader = 不同类）。

---

## 第六章：诊断工具箱

### 6.1 命令行三件套（必会）

```bash
# 1. 找进程
jps -l                    # 列出 Java 进程 PID 和主类

# 2. 看 GC（每秒刷一次，看 OG/OU 列涨不涨）
jstat -gc <pid> 1000

# 输出关键列：
#  S0C S1C S0U S1U EC EU OC OU YGC YGCT FGC FGCT GCT
#  新生代容量/使用            老代容量/使用  次数/时间 FullGC次数/时间/总时间

# 3. 看堆概览 / 导出堆快照
jmap -heap <pid>
jmap -dump:format=b,file=heap.hprof <pid>
```

### 6.2 图形化 / 离线分析

| 工具 | 用途 | 获取方式 |
|---|---|---|
| **VisualVM** | 实时监控堆、线程、CPU 采样 | JDK 自带 `jvisualvm` |
| **MAT** | 分析 heap dump，找支配树 / 引用链 | Eclipse 独立版 |
| **JFR + JMC** | 低开销飞行记录，生产可用 | JDK 自带 `-XX:StartFlightRecording` |
| Arthas | 线上诊断神器：反编译、trace、watch | 阿里开源，`curl` 下载即用 |

### 6.3 GC 日志——排障第一现场

JDK 9+ 统一日志框架：

```bash
# 开启 GC 日志（生产建议落文件）
java -Xlog:gc*:file=gc.log:time,uptime:filecount=5,filesize=10m \
     -jar app.jar

# 看懂这些就超过 80% 的人：
# [GC (Allocation Failure) [PSYoungGen: 65536K->1024K(76288K)] 
#  65536K->1024K(251392K), 0.0123456 secs]
#  含义：分配失败触发 YGC，新生代 65536K→1024K，整个堆 65536K→1024K，耗时 12ms
```

G1 典型日志：

```
[GC pause (G1 Humongous Allocation) (young) ... ]
[GC pause (G1 Evacuation Pause) (mixed) ...]   ← mixed GC，同时清老年代
[Full GC (Metadata GC Threshold) ...]          ← 危险信号，查元空间
```

---

## 第七章：性能优化方法论（流程比参数重要）

### 7.1 调优四步法

```
第 1 步：先确定症状是什么？
        ├── 接口 RT 高 / 偶发毛刺 → 疑似 GC 停顿
        ├── 吞吐下降、CPU 高      → 疑似 GC 频繁 / 死循环
        ├── 内存持续上涨          → 疑似泄漏
        └── 直接 OOM              → 看异常类型 + heap dump

第 2 步：收集证据（不要猜！）
        ├── jstat 看 FGC 频率、OU 是否只涨不跌
        ├── GC 日志看每次回收量
        └── heap dump 看谁占内存

第 3 步：定位根因
        ├── 内存泄漏 → MAT 看支配树 Top、GC Roots 引用链
        ├── GC 频繁 → 是分配太快？还是泄漏导致能回收的越来越少？
        └── CPU 高   → top -Hp <pid> 找线程 → jstack 打栈

第 4 步：改动 → 验证 → 观察（每次只改一个变量）
```

### 7.2 内存泄漏的标准排查流程

**症状**：`jstat` 里 OU（老年代使用量）**只涨不跌**，FGC 越来越频繁。

```
jstat -gc <pid> 1000
# 观察 OU 列：
# 100M → 150M → 200M → (FGC) → 190M → 210M ...   ← 泄漏
# 100M → 150M → 200M → (FGC) →  80M              ← 正常，能回收
```

**抓 dump**：

```bash
jmap -dump:live,format=b,file=heap.hprof <pid>   # live=只 dump 存活对象，更准也更慢
# 或 OOM 时自动导出：
java -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/data/ -jar app.jar
```

**用 MAT 打开**：
1. 看 **Dominator Tree**（按占用排序）——谁占大头
2. 点开大对象 → **Path to GC Roots** → 找到泄漏引用链
3. 常见泄漏点：

| 泄漏模式 | 引用链特征 | 修法 |
|---|---|---|
| 静态集合只 add 不 remove | `Class → static List → 大量对象` | 加容量上限 / 定时清理 / 用 Guava Cache |
| 监听器 / 回调没注销 | `A → listenerList → B` | `remove` 或 WeakReference |
| 连接流未关闭 | `Socket → 内部缓冲` | try-with-resources |
| ThreadLocal 未 remove（线程池场景） | `Thread → ThreadLocalMap → value` | 用完必须 `remove()` |
| 类加载器泄漏（热部署） | `ClassLoader → 全部类` | 确保旧 loader 可被回收 |

👉 **动手**：`memory.HeapOOMDemo` 配合 `-XX:+HeapDumpOnOutOfMemoryError` 生成 dump，用 MAT/VisualVM 练一遍。

### 7.3 Full GC 频繁的常见原因

```
jstat -gc <pid> 1000 看 FGC 快速上涨，依次排查：

1. 内存泄漏（最常见）        → 按 7.2 流程抓 dump
2. 元空间太小               → -XX:MaxMetaspaceSize 加大（如 256m→512m）
3. 大对象直接进老年代        → 检查有没有一次性 new 大数组
4. 显式 System.gc()        → -XX:+DisableExplicitGC（或确认是框架误调）
5. 老年代比例不合理          → 先别乱调，G1 会自适应
6. CMS 残留（老 JDK）        → Concurrent Mode Failure，升 JDK 或换 G1
```

### 7.4 CPU 100% 排查（死循环 / GC 太猛）

```bash
# 1. 找到 Java 进程 PID
top -c

# 2. 找到该进程里 CPU 最高的线程（注意 tid 是十六进制前的十进制）
top -Hp <pid>

# 3. 把 tid 转十六进制：printf '%x\n' <tid>  → 如 2a3b

# 4. jstack 打线程栈，搜 nid=0x2a3b
jstack <pid> > jstack.log
grep -A 30 'nid=0x2a3b' jstack.log
```

结果怎么读：
- 栈在 `RUNNABLE` + 你的业务代码某行死循环 → 代码 bug
- 栈大量出现在 GC 线程（`G1` / `ParGC`）→ 其实在 GC，回到 7.3
- 栈在 `WAITING` 但 CPU 高 → 通常也是 GC 或 JIT 编译线程

### 7.5 接口偶发毛刺（延迟优化）

```
P99 偶尔从 50ms 跳到 500ms：
1. 先查 GC 日志：毛刺时间点有没有 Full GC / 长 Young GC
2. 有 → 减少分配（对象池、StringBuilder 复用、流式处理改批处理）
3. 无 → 查网络、锁、慢 SQL，别急着怪 JVM
```

减少分配的常见手法：

```java
// 反例：循环里创建大量临时对象
for (Item item : list) {
    String s = "id=" + item.getId() + ",name=" + item.getName();  // 每次 new StringBuilder
    log(s);
}

// 优化1：日志占位符，避免无谓拼接
log.debug("id={},name={}", item.getId(), item.getName());

// 优化2：复用缓冲区（注意线程安全）
StringBuilder sb = new StringBuilder(64);
for (Item item : list) {
    sb.setLength(0);
    sb.append("id=").append(item.getId());
    ...
}
```

---

## 第八章：常用 JVM 参数速查

> 原则：**能不加就不加**。G1/ZGC 自适应已经很好，乱调不如不调。

### 8.1 内存（最常用）

```bash
-Xms4g                    # 堆初始大小（建议 = Xmx，避免运行时伸缩）
-Xmx4g                    # 堆最大值（物理内存的 25%~50%，别超）
-Xmn2g                    # 新生代大小（G1 下一般不设，让它自适应）
-XX:MetaspaceSize=256m    # 元空间初始阈值
-XX:MaxMetaspaceSize=512m # 元空间上限
-Xss512k                  # 线程栈（默认够用，递归深才调）
```

### 8.2 GC 选择

```bash
# JDK 8
-XX:+UseParallelGC -XX:+UseParallelOldGC    # 默认（服务端）
-XX:+UseG1GC                                # 推荐升级

# JDK 9+ 默认 G1，一般只需：
-XX:MaxGCPauseMillis=200

# 大堆低延迟
-XX:+UseZGC -Xms32g -Xmx32g
```

### 8.3 诊断与 dump

```bash
-Xlog:gc*:file=gc.log:time,uptime:filecount=5,filesize=10m
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/data/dumps/
-XX:+ExitOnOutOfMemoryError      # OOM 直接退出，交给编排系统重启
-XX:StartFlightRecording=duration=60s,filename=rec.jfr   # 飞行记录
```

### 8.4 生产环境推荐基线（JDK 17 + G1）

```bash
java -Xms4g -Xmx4g \
     -XX:MaxMetaspaceSize=512m \
     -XX:+HeapDumpOnOutOfMemoryError \
     -XX:HeapDumpPath=/data/dumps/ \
     -XX:+ExitOnOutOfMemoryError \
     -Xlog:gc*:file=/data/logs/gc.log:time,uptime:filecount=10,filesize=20m \
     -jar app.jar
```

**调参铁律**：
1. 先 `Xms = Xmx`（避免堆动态伸缩带来的抖动）
2. 没有证据不加参数；加了要能说清"为什么加、预期改善什么"
3. 每次只改一个参数，用 jstat / GC 日志验证

---

## 第九章：实战案例集

### 案例 1：静态集合内存泄漏

```java
public class UserServiceImpl implements UserService {
    // 看似无害，实际是泄漏
    private static final List<UserCache> CACHE = new ArrayList<>();

    public User getUser(String id) {
        CACHE.add(new UserCache(id, loadFromDb(id)));  // 只加不删
        return loadFromDb(id);
    }
}
```

**现象**：服务跑几天后 FGC 频繁，接口变慢，最终 OOM。
**定位**：`jstat` OU 只涨不跌 → heap dump → MAT 支配树发现 `UserServiceImpl.CACHE` 占 2GB。
**修复**：

```java
// 方案1：换成有界缓存
private static final Cache<String, User> CACHE = Caffeine.newBuilder()
        .maximumSize(10_000)
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .build();

// 方案2：ThreadLocal 必须 remove（线程池复用线程时必踩）
private static final ThreadLocal<SimpleDateFormat> FMT = ...;
try {
    ...
} finally {
    FMT.remove();   // 不 remove = 线程不死、对象不走
}
```

### 案例 2：循环内拼接导致 GC 压力大

```java
// 日志里全是这种：
// [GC pause (G1 Evacuation Pause) (young) 2048M->100M(4096M), 0.08s]
// YGC 每秒几十次，接口 RT 偶发 200ms+

for (Order order : orders) {   // orders 可能 10 万条
    String json = order.toString();      // 大量临时字符串
    reportService.send(json);            // 网络慢，字符串等在队列里
}
```

**修复**：降低分配速率 + 控制在途对象数量

```java
// 批量 + 复用 + 背压
StringBuilder sb = new StringBuilder(4096);
for (List<Order> batch : Lists.partition(orders, 500)) {
    for (Order order : batch) {
        sb.setLength(0);
        sb.append(order.getId()).append(',').append(order.getAmount());
        queue.put(sb.toString());   // 有界队列，天然背压
    }
}
```

### 案例 3：线上 OOM 应急 SOP

```
1. 先看异常类型：
   Java heap space      → 堆，走 dump 流程
   Metaspace            → 类加载泄漏，jcmd VM.classloader_stats
   Direct buffer memory → 堆外，Netty/ NIO，-XX:MaxDirectMemorySize
   GC overhead limit    → 实质还是堆满

2. heap dump 太大导不出来？
   jmap -dump:live 会触发 Full GC，低峰期再做；
   或 K8s 里直接对容器发 SIGQUIT + HeapDumpOnOutOfMemoryError

3. 分析完优先做"止血"：
   - 能快速扩容就扩容（治标）
   - 找到泄漏点修掉才是治本
   - 加上 -XX:+ExitOnOutOfMemoryError，让编排系统自动拉起
```

---

## 第十章：学习路径与自测题

### 10.1 建议学习顺序（对应本仓库实验）

| 阶段 | 内容 | 实验 |
|---|---|---|
| Day 1-2 | 内存区域、栈帧、堆结构 | `memory.StackOverflowDemo`、`memory.HeapOOMDemo` |
| Day 3 | 四种引用 | `reference.ReferenceDemo` |
| Day 4 | 类加载、双亲委派 | `classload.ClassLoaderDemo` |
| Day 5 | GC 日志怎么看、逃逸分析 | `gc.AllocationDemo` + `-Xlog:gc*` |
| Day 6-7 | MAT 分析 dump、jstat/jstack 综合 | 自己制造泄漏再排查 |
| Day 2 周+ | 读《深入理解 JVM》第 2/3/5 章 + G1/ZGC 细节 | — |

### 10.2 自测：能答上来就出师了

1. 对象一定分配在堆上吗？（TLAB、逃逸分析、标量替换）
2. `-Xms` 和 `-Xmx` 为什么建议设成一样？
3. `static int x = 123` 在准备阶段的值是多少？
4. 为什么自定义 ClassLoader 加载同一个类，`==` 是 false？
5. `ThreadLocal` 为什么在线程池里必须 `remove()`？
6. `jstat` 里 OU 只涨不跌，说明什么？下一步做什么？
7. Full GC 和 Young GC 的触发条件分别是什么？
8. 线程 CPU 100%，你的三条命令是什么？
9. G1 和 ZGC 分别适合什么场景？
10. 拿到一份 gc.log，你第一眼看哪三个字段？

---

## 附录：一句话速记卡

- 堆装对象，栈装帧，元空间装类信息
- 对象朝生夕死，所以分代；熬过 15 次 GC 晋升老年代
- Young GC 频繁正常，**Full GC 频繁才是病**
- GC Roots 可达性分析，不是引用计数
- 不同 ClassLoader 加载的同名类，是两个类
- 调优前先看 jstat / GC 日志 / dump，**不要凭感觉调参数**
- 生产基线：`Xms=Xmx` + GC 日志 + OOM 自动 dump + OOM 自动退出
- 内存泄漏的本质：**有东西一直引用着，GC Roots 走得到**
- 减少分配比调 GC 更有效
- G1 默认够用，大堆才上 ZGC

---

## 延伸阅读

- 《深入理解 Java 虚拟机》（周志明）—— 第 2 章内存、第 3 章 GC、第 7 章类加载
- [The GraalVM site](https://www.graalvm.org/) / [ZGC Docs](https://docs.oracle.com/en/java/javase/21/gctuning/)
- [Arthas 官方文档](https://arthas.aliyun.com/) —— 线上诊断必备
- [G1 GC 官方 Tuning Guide](https://docs.oracle.com/en/java/javase/17/gctuning/garbage-first-g1-garbage-collector.html)

> 文档配套代码与实验命令见仓库根目录 [README.md](./README.md)。
