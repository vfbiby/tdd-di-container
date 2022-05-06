import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.internal.util.collections.Sets;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

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
            Component instance = new Component() {
            };
            //When
            config.bind(Component.class, instance);
            //Then
            assertSame(instance, config.getContext().get(Component.class).get());
        }

        @ParameterizedTest(name = "supporting {0}")
        @MethodSource
        public void should_bind_type_to_a_injectable_component(Class<? extends Component> componentType) {
            Dependency dependency = new Dependency() {
            };
            config.bind(Dependency.class, dependency);
            config.bind(Component.class, componentType);
            Optional<Component> component = config.getContext().get(Component.class);

            assertTrue(component.isPresent());
            assertSame(dependency, component.get().dependency());
        }

        public static Stream<Arguments> should_bind_type_to_a_injectable_component() {
            return Stream.of(
                    Arguments.of(Named.of("Constructor Injection", TypeBinding.ConstructorInjection.class)),
                    Arguments.of(Named.of("Field Injection", TypeBinding.FieldInjection.class)),
                    Arguments.of(Named.of("Method Injection", TypeBinding.MethodInjection.class)));
        }

        static class ConstructorInjection implements Component {
            private final Dependency dependency;

            @Inject
            public ConstructorInjection(Dependency dependency) {
                this.dependency = dependency;
            }

            public Dependency dependency() {
                return dependency;
            }
        }

        static class FieldInjection implements Component {
            @Inject
            public Dependency dependency;

            public Dependency dependency() {
                return dependency;
            }
        }

        static class MethodInjection implements Component {
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
            Optional<Component> component = config.getContext().get(Component.class);
            assertTrue(component.isEmpty());
        }

        //could get Provider<T> from context
        @Test
        @DisplayName("should retrieve bind type as provider")
        public void should_retrieve_bind_type_as_provider() {
            Component instance = new Component() {
            };
            config.bind(Component.class, instance);
            Context context = config.getContext();

            ParameterizedType type = new TypeLiteral<Provider<Component>>() {
            }.getType();

            Provider<Component> provider = (Provider<Component>) context.get(type).get();
            assertSame(instance, provider.get());
        }

        static abstract class TypeLiteral<T> {
            public ParameterizedType getType() {
                return (ParameterizedType) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
            }
        }

        @Test
        @DisplayName("should not retrieve bind type as unsupported container")
        public void should_not_retrieve_bind_type_as_unsupported_container() {
            Component instance = new Component() {
            };
            config.bind(Component.class, instance);
            Context context = config.getContext();

            ParameterizedType type = new TypeLiteral<List<Component>>() {
            }.getType();

            assertFalse(context.get(type).isPresent());
        }
    }

    @Nested
    public class DependencyChecker {

        // dependencies not exist
        @ParameterizedTest
        @MethodSource
        public void should_throw_exception_if_dependency_not_found(Class<? extends Component> component) {
            config.bind(Component.class, component);
            DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class, () -> {
                config.getContext();
            });
            assertEquals(Dependency.class, exception.getDependency());
            assertEquals(Component.class, exception.getComponent());
        }

        public static Stream<Arguments> should_throw_exception_if_dependency_not_found() {
            return Stream.of(
                    Arguments.of(Named.of("Constructor Injection", DependencyChecker.MissingDependencyConstructor.class)),
                    Arguments.of(Named.of("Field Injection", DependencyChecker.MissingDependencyField.class)),
                    Arguments.of(Named.of("Method Injection", DependencyChecker.MissingDependencyMethod.class)),
                    Arguments.of(Named.of("Provider in Inject Constructor", MissingDependencyProviderConstructor.class)),
                    Arguments.of(Named.of("Provider in Inject Field", MissingDependencyProviderField.class)),
                    Arguments.of(Named.of("Provider in Inject Method", MissingDependencyProviderMethod.class))
            );
        }

        private static class MissingDependencyConstructor implements Component {
            @Inject
            public MissingDependencyConstructor(Dependency dependency) {
            }
        }

        private static class MissingDependencyField implements Component {
            @Inject
            public Dependency dependency;
        }

        private static class MissingDependencyMethod implements Component {
            @Inject
            void install(Dependency dependency) {
            }
        }

        static class MissingDependencyProviderConstructor implements Component {
            @Inject
            public MissingDependencyProviderConstructor(Provider<Dependency> dependency) {
            }
        }

        private static class MissingDependencyProviderField implements Component {
            @Inject
            Provider<Dependency> dependency;
        }

        private static class MissingDependencyProviderMethod implements Component {
            @Inject
            void install(Provider<Dependency> dependency) {
            }
        }

        // cycle dependencies
        @ParameterizedTest(name = "cyclic dependency between {0} and {1}")
        @MethodSource
        public void should_throw_exception_if_cyclic_dependencies_found(Class<? extends Component> component,
                                                                        Class<? extends Dependency> dependency) {
            config.bind(Component.class, component);
            config.bind(Dependency.class, dependency);

            CyclicDependencyFoundException exception = assertThrows(CyclicDependencyFoundException.class, () ->
                    config.getContext());
            Set<Class<?>> classes = Sets.newSet(exception.getComponents());

            assertEquals(2, classes.size());
            assertTrue(classes.contains(Component.class));
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

        private static class CyclicComponentInjectConstructor implements Component {
            @Inject
            public CyclicComponentInjectConstructor(Dependency dependency) {
            }
        }

        private static class CyclicComponentInjectField implements Component {
            @Inject
            Dependency dependency;
        }

        private static class CyclicComponentInjectMethod implements Component {
            @Inject
            void install(Dependency dependency) {
            }
        }

        private static class CyclicDependencyInjectConstructor implements Dependency {
            @Inject
            public CyclicDependencyInjectConstructor(Component component) {
            }
        }

        private static class CyclicDependencyInjectField implements Dependency {
            @Inject
            Component component;
        }

        private static class CyclicDependencyInjectMethod implements Dependency {
            @Inject
            void install(Component component) {
            }
        }

        @ParameterizedTest(name = "indirect cyclic dependency between {0}, {1} and {2}")
        @MethodSource
        public void should_throw_exception_if_transitive_cyclic_dependencies_found(Class<? extends Component> component,
                                                                                   Class<? extends Dependency> dependency,
                                                                                   Class<? extends AnotherDependency> anotherDependency) {
            config.bind(Component.class, component);
            config.bind(Dependency.class, dependency);
            config.bind(AnotherDependency.class, anotherDependency);

            CyclicDependencyFoundException exception = assertThrows(CyclicDependencyFoundException.class, () ->
                    config.getContext());
            List<Class<?>> components = asList(exception.getComponents());

            assertEquals(3, components.size());
            assertTrue(components.contains(Component.class));
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
            public IndirectCyclicAnotherDependencyInjectConstructor(Component component) {
            }
        }

        private static class IndirectCyclicAnotherDependencyInjectField implements AnotherDependency {
            @Inject
            Component component;
        }

        private static class IndirectCyclicAnotherDependencyInjectMethod implements AnotherDependency {
            @Inject
            void install(Component component) {
            }
        }

        private static class CyclicDependencyProviderConstructor implements Dependency {
            @Inject
            public CyclicDependencyProviderConstructor(Provider<Component> component) {
            }
        }

        @Test
        @DisplayName("should not throw exception if cyclic dependency via provider")
        public void should_not_throw_exception_if_cyclic_dependency_via_provider() {
            config.bind(Component.class, CyclicComponentInjectConstructor.class);
            config.bind(Dependency.class, CyclicDependencyProviderConstructor.class);

            Context context = config.getContext();
            assertTrue(context.get(Component.class).isPresent());
        }

    }

}
