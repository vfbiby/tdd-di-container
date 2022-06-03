import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Scope;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.internal.util.collections.Sets;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

@Nested
public class ContextTest {

    ContextConfig config;

    @BeforeEach
    public void setup() {
        config = new ContextConfig();
    }

    @Nested
    public class TypeBinding {

        @Test
        @DisplayName("should bind type to a specific instance")
        public void should_bind_type_to_a_specific_instance() {
            //Give
            TestComponent instance = new TestComponent() {
            };
            //When
            config.bind(TestComponent.class, instance);
            //Then
            assertSame(instance, config.getContext().get(ComponentRef.of(TestComponent.class)).get());
        }

        @ParameterizedTest(name = "supporting {0}")
        @MethodSource
        public void should_bind_type_to_a_injectable_component(Class<? extends TestComponent> componentType) {
            Dependency dependency = new Dependency() {
            };
            config.bind(Dependency.class, dependency);
            config.bind(TestComponent.class, componentType);
            Optional<TestComponent> component = config.getContext().get(ComponentRef.of(TestComponent.class));

            assertTrue(component.isPresent());
            assertSame(dependency, component.get().dependency());
        }

        public static Stream<Arguments> should_bind_type_to_a_injectable_component() {
            return Stream.of(
                    Arguments.of(Named.of("Constructor Injection", TypeBinding.ConstructorInjection.class)),
                    Arguments.of(Named.of("Field Injection", TypeBinding.FieldInjection.class)),
                    Arguments.of(Named.of("Method Injection", TypeBinding.MethodInjection.class)));
        }

        static class ConstructorInjection implements TestComponent {
            private final Dependency dependency;

            @Inject
            public ConstructorInjection(Dependency dependency) {
                this.dependency = dependency;
            }

            public Dependency dependency() {
                return dependency;
            }
        }

        static class FieldInjection implements TestComponent {
            @Inject
            public Dependency dependency;

            public Dependency dependency() {
                return dependency;
            }
        }

        static class MethodInjection implements TestComponent {
            private Dependency dependency;

            @Inject
            void install(Dependency dependency) {
                this.dependency = dependency;
            }

            public Dependency dependency() {
                return dependency;
            }
        }

        // component does not exist
        @Test
        public void should_retrieve_empty_for_unbind_type() {
            Optional<TestComponent> component = config.getContext().get(ComponentRef.of(TestComponent.class));
            assertTrue(component.isEmpty());
        }

        //could get Provider<T> from context
        @Test
        @DisplayName("should retrieve bind type as provider")
        public void should_retrieve_bind_type_as_provider() {
            TestComponent instance = new TestComponent() {
            };
            config.bind(TestComponent.class, instance);
            Context context = config.getContext();
            Provider<TestComponent> provider = context.get(new ComponentRef<Provider<TestComponent>>() {
            }).get();
            assertSame(instance, provider.get());
        }

        @Test
        @DisplayName("should not retrieve bind type as unsupported container")
        public void should_not_retrieve_bind_type_as_unsupported_container() {
            TestComponent instance = new TestComponent() {
            };
            config.bind(TestComponent.class, instance);
            Context context = config.getContext();

            assertFalse(context.get(new ComponentRef<List<TestComponent>>() {
            }).isPresent());
        }

        @Nested
        class WithQualifier {

            @Test
            @DisplayName("should bind instance with qualifiers")
            public void should_bind_instance_with_qualifiers() {
                TestComponent instance = new TestComponent() {
                };
                config.bind(TestComponent.class, instance, new NamedLiteral("ChosenOne"), new SkywalkerLiteral());
                Context context = config.getContext();
                TestComponent chosenOne = context.get(ComponentRef.of(TestComponent.class, new NamedLiteral("ChosenOne"))).get();
                TestComponent skywalker = context.get(ComponentRef.of(TestComponent.class, new SkywalkerLiteral())).get();

                assertSame(instance, chosenOne);
                assertSame(instance, skywalker);
            }

            @Test
            @DisplayName("should retrieve bind type as provider")
            public void should_retrieve_bind_type_as_provider() {
                TestComponent instance = new TestComponent() {
                };
                config.bind(TestComponent.class, instance, new NamedLiteral("ChoseOne"), new SkywalkerLiteral());
                Context context = config.getContext();
                Provider<TestComponent> provider = context.get(new ComponentRef<Provider<TestComponent>>(new SkywalkerLiteral()) {
                }).get();
                assertSame(instance, provider.get());
            }

            @Test
            @DisplayName("should bind component with multi qualifier")
            public void should_bind_component_with_multi_qualifier() {
                Dependency dependency = new Dependency() {
                };
                config.bind(Dependency.class, dependency);
                config.bind(InjectionTest.ComponentWithInjectConstructor.class, InjectionTest.ComponentWithInjectConstructor.class, new NamedLiteral("ChosenOne"), new SkywalkerLiteral());

                Context context = config.getContext();
                InjectionTest.ComponentWithInjectConstructor chosenOne = context.get(ComponentRef.of(InjectionTest.ComponentWithInjectConstructor.class, new NamedLiteral("ChosenOne"))).get();
                InjectionTest.ComponentWithInjectConstructor skywalker = context.get(ComponentRef.of(InjectionTest.ComponentWithInjectConstructor.class, new SkywalkerLiteral())).get();

                assertSame(dependency, chosenOne.getDependency());
                assertSame(dependency, skywalker.getDependency());
            }

            @Test
            @DisplayName("should retrieve empty if no matched qualifier")
            public void should_retrieve_empty_if_no_matched_qualifier() {
                config.bind(TestComponent.class, new TestComponent() {
                });
                Optional<TestComponent> component = config.getContext().get(ComponentRef.of(TestComponent.class, new SkywalkerLiteral()));
                assertTrue(component.isEmpty());
            }

            // throws illegal component if illegal qualifier
            @Test
            @DisplayName("should throw exception if illegal qualifier given to instance")
            public void should_throw_exception_if_illegal_qualifier_given_to_instance() {
                TestComponent instance = new TestComponent() {
                };
                assertThrows(IllegalComponentException.class, () -> config.bind(TestComponent.class, instance, new TestLiteral()));
            }

            @Test
            @DisplayName("should throw exception if illegal qualifier given to component")
            public void should_throw_exception_if_illegal_qualifier_given_to_component() {
                assertThrows(IllegalComponentException.class, () -> config.bind(TestComponent.class, InjectionTest.ComponentWithInjectConstructor.class, new TestLiteral()));
            }

        }

        @Nested
        class WithScope {
            // default scope should not be singleton
            static class NotSingleton {
            }

            @Test
            @DisplayName("should not be singleton scope by default")
            public void should_not_be_singleton_scope_by_default() {
                config.bind(NotSingleton.class, NotSingleton.class);
                Context context = config.getContext();
                assertNotSame(context.get(ComponentRef.of(NotSingleton.class)), context.get(ComponentRef.of(NotSingleton.class)));
            }

            // bind component as singleton scoped
            @Test
            @DisplayName("should bind component as singleton scoped")
            public void should_bind_component_as_singleton_scoped() {
                config.bind(NotSingleton.class, NotSingleton.class, new SingletonLiteral());
                Context context = config.getContext();
                assertSame(context.get(ComponentRef.of(NotSingleton.class)).get(), context.get(ComponentRef.of(NotSingleton.class)).get());
            }

            // bind component with qualifiers as singleton scoped
            @Singleton
            static class SingletonAnnotated implements Dependency {
            }

            // get scope from component class
            @Test
            @DisplayName("should retrieve scope annotation from component")
            public void should_retrieve_scope_annotation_from_component() {
                config.bind(Dependency.class, SingletonAnnotated.class);
                Context context = config.getContext();
                assertSame(context.get(ComponentRef.of(Dependency.class)).get(), context.get(ComponentRef.of(Dependency.class)).get());
            }


            @Nested
            class WithQualifier {
                @Test
                @DisplayName("should not be singleton scope by default")
                public void should_not_be_singleton_scope_by_default() {
                    config.bind(NotSingleton.class, NotSingleton.class, new SkywalkerLiteral());
                    Context context = config.getContext();
                    assertNotSame(context.get(ComponentRef.of(NotSingleton.class, new SkywalkerLiteral())),
                            context.get(ComponentRef.of(NotSingleton.class, new SkywalkerLiteral())));
                }

                // bind component with qualifiers as singleton scoped
                @Test
                @DisplayName("should bind component as singleton scoped")
                public void should_bind_component_as_singleton_scoped() {
                    config.bind(NotSingleton.class, NotSingleton.class, new SingletonLiteral());
                    Context context = config.getContext();
                    assertSame(context.get(ComponentRef.of(NotSingleton.class)).get(), context.get(ComponentRef.of(NotSingleton.class)).get());
                }

                // get scope from component with qualifiers
                @Test
                @DisplayName("should retrieve scope annotation from component")
                public void should_retrieve_scope_annotation_from_component() {
                    config.bind(Dependency.class, SingletonAnnotated.class, new SkywalkerLiteral());
                    Context context = config.getContext();
                    assertSame(context.get(ComponentRef.of(Dependency.class, new SkywalkerLiteral())).get(),
                            context.get(ComponentRef.of(Dependency.class, new SkywalkerLiteral())).get());
                }

            }

            // bind component with customize scope annotation
            @Test
            @DisplayName("should bind component as customized scope")
            public void should_bind_component_as_customized_scope() {
                config.scope(Pooled.class, PooledProvider::new);
                config.bind(NotSingleton.class, NotSingleton.class, new PooledLiteral());
                Context context = config.getContext();
                List<NotSingleton> instances = IntStream.range(0, 5).mapToObj(i -> context.get(ComponentRef.of(NotSingleton.class)).get()).toList();
                assertEquals(PooledProvider.MAX, new HashSet<>(instances).size());
            }

        }

    }

    @Nested
    public class DependencyChecker {

        // dependencies not exist
        @ParameterizedTest
        @MethodSource
        public void should_throw_exception_if_dependency_not_found(Class<? extends TestComponent> component) {
            config.bind(TestComponent.class, component);
            DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class, () -> {
                config.getContext();
            });
            assertEquals(Dependency.class, exception.getDependency().type());
            assertEquals(TestComponent.class, exception.getComponent().type());
        }

        public static Stream<Arguments> should_throw_exception_if_dependency_not_found() {
            return Stream.of(
                    Arguments.of(Named.of("Constructor Injection", DependencyChecker.MissingDependencyConstructor.class)),
                    Arguments.of(Named.of("Field Injection", DependencyChecker.MissingDependencyField.class)),
                    Arguments.of(Named.of("Method Injection", DependencyChecker.MissingDependencyMethod.class)),
                    Arguments.of(Named.of("Provider in Inject Constructor", MissingDependencyProviderConstructor.class)),
                    Arguments.of(Named.of("Provider in Inject Field", MissingDependencyProviderField.class)),
                    Arguments.of(Named.of("Provider in Inject Method", MissingDependencyProviderMethod.class)),
                    Arguments.of(Named.of("Scoped", MissingDependencyScoped.class)),
                    Arguments.of(Named.of("Scoped Provider", MissingDependencyProviderScoped.class))
            );
        }

        // missing dependencies with scope

        static class MissingDependencyConstructor implements TestComponent {
            @Inject
            public MissingDependencyConstructor(Dependency dependency) {
            }
        }

        static class MissingDependencyField implements TestComponent {
            @Inject
            public Dependency dependency;
        }

        static class MissingDependencyMethod implements TestComponent {
            @Inject
            void install(Dependency dependency) {
            }
        }

        static class MissingDependencyProviderConstructor implements TestComponent {
            @Inject
            public MissingDependencyProviderConstructor(Provider<Dependency> dependency) {
            }
        }

        static class MissingDependencyProviderField implements TestComponent {
            @Inject
            Provider<Dependency> dependency;
        }

        static class MissingDependencyProviderMethod implements TestComponent {
            @Inject
            void install(Provider<Dependency> dependency) {
            }
        }

        static class MissingDependencyScoped implements TestComponent {
            @Inject
            Dependency dependency;
        }

        static class MissingDependencyProviderScoped implements TestComponent {
            @Inject
            Provider<Dependency> dependency;
        }

        // cycle dependencies
        @ParameterizedTest(name = "cyclic dependency between {0} and {1}")
        @MethodSource
        public void should_throw_exception_if_cyclic_dependencies_found(Class<? extends TestComponent> component,
                                                                        Class<? extends Dependency> dependency) {
            config.bind(TestComponent.class, component);
            config.bind(Dependency.class, dependency);

            CyclicDependencyFoundException exception = assertThrows(CyclicDependencyFoundException.class, () ->
                    config.getContext());
            Set<Class<?>> classes = Sets.newSet(exception.getComponents());

            assertEquals(2, classes.size());
            assertTrue(classes.contains(TestComponent.class));
            assertTrue(classes.contains(Dependency.class));
        }

        public static final Stream<Arguments> should_throw_exception_if_cyclic_dependencies_found() {
            ArrayList<Arguments> arguments = new ArrayList<>();
            for (Named component : List.of(
                    Named.of("Constructor Injection", DependencyChecker.CyclicComponentInjectConstructor.class),
                    Named.of("Field Injection", DependencyChecker.CyclicComponentInjectField.class),
                    Named.of("Method Injection", DependencyChecker.CyclicComponentInjectMethod.class)))
                for (Named dependency : List.of(
                        Named.of("Constructor Injection", DependencyChecker.CyclicDependencyInjectConstructor.class),
                        Named.of("Field Injection", DependencyChecker.CyclicDependencyInjectField.class),
                        Named.of("Method Injection", DependencyChecker.CyclicDependencyInjectMethod.class)))
                    arguments.add(Arguments.of(component, dependency));
            return arguments.stream();
        }

        private static class CyclicComponentInjectConstructor implements TestComponent {
            @Inject
            public CyclicComponentInjectConstructor(Dependency dependency) {
            }
        }

        private static class CyclicComponentInjectField implements TestComponent {
            @Inject
            Dependency dependency;
        }

        private static class CyclicComponentInjectMethod implements TestComponent {
            @Inject
            void install(Dependency dependency) {
            }
        }

        private static class CyclicDependencyInjectConstructor implements Dependency {
            @Inject
            public CyclicDependencyInjectConstructor(TestComponent component) {
            }
        }

        private static class CyclicDependencyInjectField implements Dependency {
            @Inject
            TestComponent component;
        }

        private static class CyclicDependencyInjectMethod implements Dependency {
            @Inject
            void install(TestComponent component) {
            }
        }

        @ParameterizedTest(name = "indirect cyclic dependency between {0}, {1} and {2}")
        @MethodSource
        public void should_throw_exception_if_transitive_cyclic_dependencies_found(Class<? extends TestComponent> component,
                                                                                   Class<? extends Dependency> dependency,
                                                                                   Class<? extends AnotherDependency> anotherDependency) {
            config.bind(TestComponent.class, component);
            config.bind(Dependency.class, dependency);
            config.bind(AnotherDependency.class, anotherDependency);

            CyclicDependencyFoundException exception = assertThrows(CyclicDependencyFoundException.class, () ->
                    config.getContext());
            List<Class<?>> components = asList(exception.getComponents());

            assertEquals(3, components.size());
            assertTrue(components.contains(TestComponent.class));
            assertTrue(components.contains(Dependency.class));
            assertTrue(components.contains(AnotherDependency.class));
        }

        public static Stream<Arguments> should_throw_exception_if_transitive_cyclic_dependencies_found() {
            ArrayList<Arguments> arguments = new ArrayList<>();
            for (Named component : List.of(
                    Named.of("Inject Constructor", DependencyChecker.CyclicComponentInjectConstructor.class),
                    Named.of("Inject Field", DependencyChecker.CyclicComponentInjectField.class),
                    Named.of("Inject Method", DependencyChecker.CyclicComponentInjectMethod.class)))
                for (Named dependency : List.of(
                        Named.of("Inject Constructor", DependencyChecker.IndirectCyclicDependencyInjectConstructor.class),
                        Named.of("Inject Field", DependencyChecker.IndirectCyclicDependencyInjectField.class),
                        Named.of("Inject Method", DependencyChecker.IndirectCyclicDependencyInjectMethod.class)))
                    for (Named anotherDependency : List.of(
                            Named.of("Inject Constructor", DependencyChecker.IndirectCyclicAnotherDependencyInjectConstructor.class),
                            Named.of("Inject Field", DependencyChecker.IndirectCyclicAnotherDependencyInjectField.class),
                            Named.of("Inject Method", DependencyChecker.IndirectCyclicAnotherDependencyInjectMethod.class)))
                        arguments.add(Arguments.of(component, dependency, anotherDependency));
            return arguments.stream();
        }

        private static class IndirectCyclicDependencyInjectConstructor implements Dependency {
            @Inject
            public IndirectCyclicDependencyInjectConstructor(AnotherDependency anotherDependency) {
            }
        }

        private static class IndirectCyclicDependencyInjectField implements Dependency {
            @Inject
            AnotherDependency anotherDependency;
        }

        private static class IndirectCyclicDependencyInjectMethod implements Dependency {
            @Inject
            void install(AnotherDependency anotherDependency) {
            }
        }

        private static class IndirectCyclicAnotherDependencyInjectConstructor implements AnotherDependency {
            @Inject
            public IndirectCyclicAnotherDependencyInjectConstructor(TestComponent component) {
            }
        }

        private static class IndirectCyclicAnotherDependencyInjectField implements AnotherDependency {
            @Inject
            TestComponent component;
        }

        private static class IndirectCyclicAnotherDependencyInjectMethod implements AnotherDependency {
            @Inject
            void install(TestComponent component) {
            }
        }

        private static class CyclicDependencyProviderConstructor implements Dependency {
            @Inject
            public CyclicDependencyProviderConstructor(Provider<TestComponent> component) {
            }
        }

        @Test
        @DisplayName("should not throw exception if cyclic dependency via provider")
        public void should_not_throw_exception_if_cyclic_dependency_via_provider() {
            config.bind(TestComponent.class, CyclicComponentInjectConstructor.class);
            config.bind(Dependency.class, CyclicDependencyProviderConstructor.class);

            Context context = config.getContext();
            assertTrue(context.get(ComponentRef.of(TestComponent.class)).isPresent());
        }

        @Nested
        class WithQualifier {
            // dependency missing if qualifier not match
            static class InjectConstructor {
                @Inject
                public InjectConstructor(@Skywalker Dependency dependency) {
                }
            }

            @ParameterizedTest
            @MethodSource
            public void should_throw_exception_if_dependency_with_qualifier_not_found(Class<? extends TestComponent> component) {
                config.bind(Dependency.class, new Dependency() {
                });
                config.bind(TestComponent.class, component, new NamedLiteral("Owner"));
                DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class, () -> config.getContext());
                assertEquals(new Component(TestComponent.class, new NamedLiteral("Owner")), exception.getComponent());
                assertEquals(new Component(Dependency.class, new SkywalkerLiteral()), exception.getDependency());
            }

            public static Stream<Arguments> should_throw_exception_if_dependency_with_qualifier_not_found() {
                return Stream.of(
                        Named.of("Inject Constructor with Qualifier", InjectConstructor.class),
                        Named.of("Inject Field with Qualifier", InjectField.class),
                        Named.of("Inject Method with Qualifier", InjectMethod.class),
                        Named.of("Provider in Inject Constructor with Qualifier", InjectConstructorProvider.class),
                        Named.of("Provider in Inject Field with Qualifier", InjectFieldProvider.class),
                        Named.of("Provider in Inject Method with Qualifier", InjectMethodProvider.class)
                ).map(Arguments::of);
            }

            @ParameterizedTest(name = "{1} -> @Skywalker({0}) -> @Named(\"ChoseOne\") not cyclic dependencies")
            @MethodSource
            public void should_not_throw_cyclic_exception_if_component_with_same_type_taged_with_different_qualifier(Class<? extends Dependency> skywalker,
                                                                                                                     Class<? extends Dependency> notCyclic) {
                Dependency instance = new Dependency() {
                };
                config.bind(Dependency.class, instance, new NamedLiteral("ChoseOne"));
                config.bind(Dependency.class, skywalker, new SkywalkerLiteral());
                config.bind(Dependency.class, notCyclic);
                assertDoesNotThrow(() -> config.getContext());
            }

            static class SkywalkerInjectConstructor implements Dependency {
                @Inject
                public SkywalkerInjectConstructor(@jakarta.inject.Named("ChoseOne") Dependency dependency) {
                }
            }

            static class SkywalkerInjectField implements Dependency {
                @Inject
                @jakarta.inject.Named("ChoseOne")
                Dependency dependency;
            }

            static class SkywalkerInjectMethod implements Dependency {
                @Inject
                void install(@jakarta.inject.Named("ChoseOne") Dependency dependency) {
                }
            }

            static class NotCyclicInjectConstructor implements Dependency {
                @Inject
                public NotCyclicInjectConstructor(@Skywalker Dependency dependency) {
                }
            }


            static class NotCyclicInjectField implements Dependency {
                @Inject
                @Skywalker
                Dependency dependency;
            }


            static class NotCyclicInjectMethod implements Dependency {
                @Inject
                public void install(@Skywalker Dependency dependency) {
                }
            }


            public static Stream<Arguments> should_not_throw_cyclic_exception_if_component_with_same_type_taged_with_different_qualifier() {
                List<Arguments> arguments = new ArrayList<>();
                for (Named skywalker : List.of(Named.of("Inject Constructor", SkywalkerInjectConstructor.class),
                        Named.of("Inject Field", SkywalkerInjectField.class),
                        Named.of("Inject Method", SkywalkerInjectMethod.class)))
                    for (Named notCyclic : List.of(Named.of("Inject Constructor", NotCyclicInjectConstructor.class),
                            Named.of("Inject Constructor", NotCyclicInjectField.class),
                            Named.of("Inject Constructor", NotCyclicInjectMethod.class)))
                        arguments.add(Arguments.of(skywalker, notCyclic));
                return arguments.stream();
            }

            static class InjectField {
                @Inject
                @Skywalker
                Dependency dependency;
            }

            static class InjectMethod {
                @Inject
                void install(@Skywalker Dependency dependency) {
                }
            }

            static class InjectConstructorProvider implements TestComponent {
                @Inject
                public InjectConstructorProvider(@Skywalker Provider<Dependency> dependency) {
                }
            }

            static class InjectFieldProvider {
                @Inject
                @Skywalker
                Provider<Dependency> dependency;
            }

            static class InjectMethodProvider {
                @Inject
                void install(@Skywalker Provider<Dependency> dependency) {
                }
            }
        }

    }
}

@java.lang.annotation.Documented
@java.lang.annotation.Retention(RetentionPolicy.RUNTIME)
@jakarta.inject.Qualifier
@interface Skywalker {
}

record NamedLiteral(String value) implements jakarta.inject.Named {
    @Override
    public Class<? extends Annotation> annotationType() {
        return jakarta.inject.Named.class;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof jakarta.inject.Named named) return Objects.equals(value, named.value());
        return false;
    }

    @Override
    public int hashCode() {
        return "value".hashCode() * 127 ^ value.hashCode();
    }
}

record SingletonLiteral() implements Singleton {

    @Override
    public Class<? extends Annotation> annotationType() {
        return Singleton.class;
    }
}

record SkywalkerLiteral() implements Skywalker {

    @Override
    public Class<? extends Annotation> annotationType() {
        return Skywalker.class;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Skywalker;
    }
}

record TestLiteral() implements Test {

    @Override
    public Class<? extends Annotation> annotationType() {
        return Test.class;
    }
}

@Scope
@Documented
@Retention(RUNTIME)
@interface Pooled {
}

record PooledLiteral() implements Pooled {

    @Override
    public Class<? extends Annotation> annotationType() {
        return Pooled.class;
    }
}

class PooledProvider<T> implements ContextConfig.ComponentProvider<T> {
    static int MAX = 2;
    private List<T> pool = new ArrayList<>();
    int current;
    private ContextConfig.ComponentProvider<T> provider;

    public PooledProvider(ContextConfig.ComponentProvider<T> provider) {
        this.provider = provider;
    }

    @Override
    public T get(Context context) {
        if (pool.size() < MAX) pool.add(provider.get(context));
        return pool.get(current++ % MAX);
    }

    @Override
    public List<ComponentRef<?>> getDependencies() {
        return provider.getDependencies();
    }
}
