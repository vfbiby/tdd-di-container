# 徐昊 · TDD 项目实战 70 讲
> 这是跟着这个课做的笔记和练习，基本上是边看边记笔记，笔记是为了后面再练的时候，不用再回去翻视频，为什么不愿意去翻视频的呢？主要是那个播放器设置的太垃圾，这么久了还没有改好。哈哈，

所以的心法都在doc下面，每一章都有，我先传一部分上来，大家看看，照着这个能不能也不用看视频就能跟着做。我尽量把这个写得更好点。不过是我自己写的，可能我自己比较能看懂。

### 大概的效果是下面这样的

# 22 | DI Container (10): 怎样重构测试代码？

> 现在的任务列表：暂时跟前一节没有什么改变，主要是做了重构

## 重构提取的InjectionTest

### 让测试的形式一致

#### 优化测试 should_bind_type_to_a_class_with_default_constructor

- 把config.bind里面的Component.class提取成一个变量
- 把实现类也提取成一个变量
- 把bind和getContext两句提取成一个方法
- 处理提取的函数的泛型，<T, R extends R>，跑一下测试

```java
private <T, R extends T> T getComponent(Class<T> type, Class<R> implementation) {
    config.bind(type, implementation);
    T component = config.getContext().get(type).get();
    return component;
}
```

- 把提取的两个变量inline回去

```java
Component instance = getComponent(Component.class, ComponentWithDefaultConstructor.class);
```

#### 修改测试 should_bind_type_to_a_class_with_inject_constructor

- 复制上面提取的方法到这个测试里面
- 删除掉之前的获取方法，只留场景准备和绑定的代码

```java
@Test
@DisplayName("should bind type to a class with inject constructor")
public void should_bind_type_to_a_class_with_inject_constructor() {
    Dependency dependency = new Dependency() {
    };
    config.bind(Dependency.class, dependency);
    Component instance = getComponent(Component.class, ComponentWithInjectConstructor.class);
    assertNotNull(instance);
    assertSame(dependency, ((ComponentWithInjectConstructor) instance).getDependency());
}
```

#### 修改测试 should_bind_type_to_a_class_with_transitive_dependencies

- 也是复制方法过来，把bind Component和ComponentWithInjectConstructor的方法名，直接改成了上面提取的方法
- 删除掉了之前获取的方法，跑测试

```java
@Test
@DisplayName("should bind type to a class with transitive dependencies")
public void should_bind_type_to_a_class_with_transitive_dependencies() {
    config.bind(Dependency.class, DependencyWithInjectConstructor.class);
    config.bind(String.class, "indirect dependency");
                                                                                                        
    Component instance = getComponent(Component.class, ComponentWithInjectConstructor.class);
    assertNotNull(instance);
                                                                                                        
    Dependency dependency = ((ComponentWithInjectConstructor) instance).getDependency();
    assertNotNull(dependency);
    assertEquals("indirect dependency", ((DependencyWithInjectConstructor) dependency).getDependency());
}
```