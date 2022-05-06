# 25 | DI Container (13): 任务上的遗漏该怎么处理？

任务列表如下：

- ~~无需构造的组件——组件实例~~
- ~~如果注册的组件不可实例化，则抛出异常~~
    - ~~抽象类~~
    - ~~接口~~
- ~~构造函数注入~~
    - ~~无依赖的组件应该通过默认构造函数生成组件实例~~
    - ~~有依赖的组件，通过 Inject 标注的构造函数生成组件实例~~
    - ~~如果所依赖的组件也存在依赖，那么需要对所依赖的组件也完成依赖注入~~
    - ~~如果组件有多于一个 Inject 标注的构造函数，则抛出异常~~
    - ~~如果组件需要的依赖不存在，则抛出异常~~
    - ~~如果组件间存在循环依赖，则抛出异常~~
- ~~字段注入~~
    - ~~通过 Inject 标注将字段声明为依赖组件~~
    - ~~如果组件需要的依赖不存在，则抛出异常~~
    - ~~常如果字段为 final 则抛出异常~~
    - ~~如果组件间存在循环依赖，则抛出异常~~
- ~~方法注入~~
    - ~~通过 Inject 标注的方法，其参数为依赖组件~~
    - ~~通过 Inject 标注的无参数方法，会被调用~~
    - ~~按照子类中的规则，覆盖父类中的 Inject 方法~~
    - ~~如果组件需要的依赖不存在，则抛出异常~~
    - 如果方法定义类型参数，则抛出异常
    - ~~如果组件间存在循环依赖，则抛出异常~~

- ~~对 Provider 类型的依赖~~
    - ~~从容器中取得组件的Provider（新增任务）~~
    - ~~注入构造函数中可以声明对于 Provider 的依赖~~
    - ~~注入字段中可以声明对于 Provider 的依赖~~
    - ~~注入方法中可声明对于 Provider 的依赖~~
- 自定义 Qualifier 的依赖
    - 注册组件时，可额外指定 Qualifier
    - 注册组件时，可从类对象上提取 Qualifier
    - 寻找依赖时，需同时满足类型与自定义 Qualifier 标注
    - 支持默认 Qualifier——Named

## 在ContextTest增加MissingDependency场景

### 添加场景类

- 添加static class MissingDepndencyProviderConstructor implements Component
- 添加一个Inject的构造函数，参数是Provider<Dependency> dependency

### 添加测试Case: Provider in Inject Constructor

测试步骤：

- 在*should_throw_exception_if_dependency_not_found*添加一种测试case，Provider in Inject Constructor这种情况
- 在数据准备的方法里面添加一种"Provider in Inject Constructor", 类型是刚创建的类型

实现步骤：

- 在interface ComponentProvider上面添加一个新的getDependencyTypes(),返回值为List<Type>
- 之前有一个getDependencies，需要加上一个default，并且return List.of()，（视频看到这里时才发现有一点点不一样）
- 在数据准备那里先添加两个TODO
- provider in inject field
- provider in inject method
- 在InjectionTest添加任务
- 在ConstructorInjection 测试里面，添加一个 //TODO include dependency type from inject constructor
- 在FieldInjection里面添加测试//TODO include dependency type from inject field
- 在FieldInjection里面添加测试//TODO include dependency type from inject method
- 把上面添加的那个测试场景注释掉，以免让测试代码一直处在failed状态，并且在前面添加一个TODO，因为想要一次性让这个测试通过不太容易，所以先从后面新中的任务开始

##### 实现 include dependency type from inject constructor场景

添加测试：在InjectionTest里面的Injection测试添加should_include_dependency_type_from_inject_constructor

测试步骤：

- new InjectionProvider，把ProviderInjectConstructor.class作为参数，赋值给provider
- 断言两个数组相等new Type[]{dependencyProviderType}, provider.getDependencyTypes().toArray(Type[]::new)
- 把之前的mock的dependencyType提取成一个field，让这里也能访问，并且改名成dependencyProviderType

```java
@Test
@DisplayName("should include provider type from inject constructor")
public void should_include_provider_type_from_inject_constructor() {
    InjectionProvider<ProviderInjectConstructor> provider = new InjectionProvider<>(ProviderInjectConstructor.class);
    assertArrayEquals(new Type[]{dependencyProviderType}, provider.getDependencyTypes().toArray(Type[]::new));
}
```

实现步骤：

- 在子类override刚才创建的getDependencyTypes
- injectConstructor.getParameters()map一下，并且获取每一个参数的getParameterizedType再toList后返回
- 跑测试通过后，把测试名改成should_include_provider_type_from_inject_constructor

##### 实现 include dependency type from inject field

添加测试：在FieldInjection的Injection里面添加测试should_include_provider_type_from_inject_field

测试步骤：

- 复制上面的测试下来改一改
- 把泛型类改成ProviderInjectField，然后跑测试

```java
@Test
@DisplayName("should include provider type from inject field")
public void should_include_provider_type_from_inject_field() {
    InjectionProvider<ProviderInjectField> provider = new InjectionProvider<>(ProviderInjectField.class);
    assertArrayEquals(new Type[]{dependencyProviderType}, provider.getDependencyTypes().toArray(Type[]::new));
}
```



实现步骤：

- 在getDependencyTypes里面，把之前的injectConstructor的内容，和injectFields的内容concat起来
- injectFields map一下，用Field::getGenericType作为处理函数
- 然后把concat后的内容toList一下

```java
@Override
public List<Type> getDependencyTypes() {
    return concat(stream(injectConstructor.getParameters()).map(Parameter::getParameterizedType),
            injectFields.stream().map(Field::getGenericType)).toList();
}
```

##### 处理 include dependency type from inject method场景

测试步骤：

- 从上面复制测试过来，改名成should_include_provider_type_from_inject_method
- 把泛型改成ProviderInjecMethod

```java
@Test
@DisplayName("should include provider type from inject method")
public void should_include_provider_type_from_inject_method() {
    InjectionProvider<ProviderInjectMethod> provider = new InjectionProvider<>(ProviderInjectMethod.class);
    assertArrayEquals(new Type[]{dependencyProviderType}, provider.getDependencyTypes().toArray(Type[]::new));
}
```



实现步骤：

- 先把injectMethods flatMap，在里面再m.getParameters map一下，用p.getParameterizedType处理
- 再把上面取得的concat一下，跑测试是通过的，

```java
@Override
public List<Type> getDependencyTypes() {
    return concat(concat(stream(injectConstructor.getParameters()).map(Parameter::getParameterizedType),
                    injectFields.stream().map(Field::getGenericType)),
            injectMethods.stream().flatMap(m -> stream(m.getParameters()).map(Parameter::getParameterizedType))).toList();
}
```



### 处理 Provider in Inject Constructor 场景

测试步骤：

- 把之前注释掉的已经写了测试数据的TODO放出来，Arguments.of(Named.of("Provider in Inject Constructor", MissingDependencyProviderConstructor.class)))，然后跑测试，有错误。

实现步骤：

- 在ContextConfig的checkDependencies里面，把for循环里面的代码抽取一个方法checkDependency
- 把for循环里面的dependency的类型改成Type，同时改成调用getDependencyTypes
- 在for里面判断，如果dependency instanceof Class，那就把提取方法里面的dependency强转成Class<?>
- 如果dependency instanceof ParameterizedType的话，先在dependency上，getActualTypeArguments()[0]，然后赋值给Class<?> type
- 如果providers里面不包含这个type的key，那就抛出DependencyNotFoundException异常（从抽取方法里面复制出来），注意，复制上来时，要把dependency和component的位置搞对，把dependency的位置改成type，再跑测试就通过了

```java
public void checkDependencies(Class<?> component, Stack<Class<?>> visiting) {
    for (Type dependency : providers.get(component).getDependencyTypes()) {
        if (dependency instanceof Class)
            checkDependency(component, visiting, (Class<?>) dependency);
        if (dependency instanceof ParameterizedType) {
            Class<?> type = (Class<?>) ((ParameterizedType) dependency).getActualTypeArguments()[0];
            if (!providers.containsKey(type)) throw new DependencyNotFoundException(type, component);
        }
    }
}
                                                                                                         
private void checkDependency(Class<?> component, Stack<Class<?>> visiting, Class<?> dependency) {
    if (!providers.containsKey(dependency)) throw new DependencyNotFoundException(dependency, component);
    if (visiting.contains(dependency)) throw new CyclicDependencyFoundException(visiting);
    visiting.push(dependency);
    checkDependencies(dependency, visiting);
    visiting.pop();
}
```



### 处理 Provider in inject field | method 场景

##### 添加测试场景数据(在should_throw_exception_if_dependency_not_found下面)

- 添加MissingDependencyProviderField implements Component，里面只有一个@inject的Provider<Dependency> dependency
- 添加MissingDependencyProviderMethod implements Component，里面有一个@Inject的void install方法，参数是Provider<Dependency> dependency，方法内没有东西

```java
private static class MissingDependencyProviderField implements Component {
    @Inject
    Provider<Dependency> dependency;
}
                                                                           
private static class MissingDependencyProviderMethod implements Component {
    @Inject
    void install(Provider<Dependency> dependency) {
    }
}
```



测试步骤：

- 添加两个测试数据源
- Arguments.of(Named.of("Provider in Inject Field", MissingDependencyProviderField.class))
- Arguments.of(Named.of("Provider in Inject Method", MissingDependencyProviderMethod.class))

- 跑测试就可以通过了，去掉TODO

```java
Arguments.of(Named.of("Provider in Inject Field", MissingDependencyProviderField.class)),
Arguments.of(Named.of("Provider in Inject Method", MissingDependencyProviderMethod.class))
```



### 处理循环依赖的场景

##### 添加测试场景数据(ContextTest)最后面

- static class CyclicDependencyProviderConstructor implements Dependency，里面有一个@Inject的constructor，依赖Provider<Component> component

```java
private static class CyclicDependencyProviderConstructor implements Dependency {
    @Inject
    public CyclicDependencyProviderConstructor(Provider<Component> component) {
    }
}
```



##### 添加测试 should_not_throw_exception_if_cyclic_dependency_via_provider

测试步骤：

- config.bind Component.class to CyclicComponentInjectConstructor.class
- config.bind Dependency.class to CyclicDependencyProviderConstructor.class
- config.getContext()赋值给context
- 断言context.get(Component.class).isPresent()为true

```java
@Test
@DisplayName("should not throw exception if cyclic dependency via provider")
public void should_not_throw_exception_if_cyclic_dependency_via_provider() {
    config.bind(Component.class, CyclicComponentInjectConstructor.class);
    config.bind(Dependency.class, CyclicDependencyProviderConstructor.class);
                                                                             
    Context context = config.getContext();
    assertTrue(context.get(Component.class).isPresent());
}
```


