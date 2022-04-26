import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.internal.util.collections.Sets;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

public class ContainerTest {

    ContextConfig config;

    @BeforeEach
    public void setup() {
        config = new ContextConfig();
    }

    @Nested
    public class ComponentConstructionTest {
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


        // TODO: 2022/4/19 abstract class
        // TODO: 2022/4/19 interface
        // component does not exist
        @Test
        public void should_return_empty_if_component_not_found() {
            Optional<Component> component = config.getContext().get(Component.class);
            assertTrue(component.isEmpty());
        }

        @Nested
        public class ConstructorInjectionTest {
            // No args constructor
            @Test
            @DisplayName("should bind type to a class with default constructor")
            public void should_bind_type_to_a_class_with_default_constructor() {
                //When
                config.bind(Component.class, ComponentWithDefaultConstructor.class);
                Component instance = config.getContext().get(Component.class).get();
                //Then
                assertNotNull(instance);
                assertTrue(instance instanceof ComponentWithDefaultConstructor);
            }

            // with dependencies
            @Test
            @DisplayName("should bind type to a class with inject constructor")
            public void should_bind_type_to_a_class_with_inject_constructor() {
                Dependency dependency = new Dependency() {
                };

                config.bind(Component.class, ComponentWithInjectConstructor.class);
                config.bind(Dependency.class, dependency);

                Component instance = config.getContext().get(Component.class).get();
                assertNotNull(instance);
                assertSame(dependency, ((ComponentWithInjectConstructor) instance).getDependency());
            }

            // A -> B -> C
            @Test
            @DisplayName("should bind type to a class with transitive_dependencies")
            public void should_bind_type_to_a_class_with_transitive_dependencies() {
                config.bind(Component.class, ComponentWithInjectConstructor.class);
                config.bind(Dependency.class, DependencyWithInjectConstructor.class);
                config.bind(String.class, "indirect dependency");

                Component instance = config.getContext().get(Component.class).get();
                assertNotNull(instance);

                Dependency dependency = ((ComponentWithInjectConstructor) instance).getDependency();
                assertNotNull(dependency);

                assertEquals("indirect dependency", ((DependencyWithInjectConstructor) dependency).getDependency());
            }

            // multi inject constructors
            @Test
            @DisplayName("should throw exception if multi inject constructors provided")
            public void should_throw_exception_if_multi_inject_constructors_provided() {
                assertThrows(IllegalComponentException.class, () -> {
                    config.bind(Component.class, ComponentWithMultiInjectConstructors.class);
                });
            }

            // no default constructor and inject constructor
            @Test
            @DisplayName("should throw exception if no inject nor default constructor provided")
            public void should_throw_exception_if_no_inject_nor_default_constructor_provided() {
                assertThrows(IllegalComponentException.class, () -> {
                    config.bind(Component.class, ComponentWithNoInjectConstructorNorDefaultConstructor.class);
                });
            }

            // dependencies not exist
            @Test
            @DisplayName("should throw exception if dependency not found")
            public void should_throw_exception_if_dependency_not_found() {
                config.bind(Component.class, ComponentWithInjectConstructor.class);
                DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class, () -> {
                    config.getContext();
                });
                assertEquals(Dependency.class, exception.getDependency());
                assertEquals(Component.class, exception.getComponent());
            }

            @Test
            @DisplayName("should throw exception if transitive dependency not found")
            public void should_throw_exception_if_transitive_dependency_not_found() {
                config.bind(Component.class, ComponentWithInjectConstructor.class);
                config.bind(Dependency.class, DependencyWithInjectConstructor.class);

                DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class, () -> {
                    config.getContext();
                });
                assertEquals(String.class, exception.getDependency());
                assertEquals(Dependency.class, exception.getComponent());
            }

            // cycle dependencies
            @Test
            @DisplayName("should throw exception if cyclic dependencies found")
            public void should_throw_exception_if_cyclic_dependencies_found() {
                config.bind(Component.class, ComponentWithInjectConstructor.class);
                config.bind(Dependency.class, DependencyDependedOnComponent.class);

                CyclicDependencyFoundException exception = assertThrows(CyclicDependencyFoundException.class, () ->
                        config.getContext());
                Set<Class<?>> classes = Sets.newSet(exception.getComponents());

                assertEquals(2, classes.size());
                assertTrue(classes.contains(Component.class));
                assertTrue(classes.contains(Dependency.class));
            }

            @Test
            @DisplayName("should throw exception if transitive cyclic dependencies found")
            public void should_throw_exception_if_transitive_cyclic_dependencies_found() {
                config.bind(Component.class, ComponentWithInjectConstructor.class);
                config.bind(Dependency.class, DependencyDependedOnAnotherDependency.class);
                config.bind(AnotherDependency.class, AnotherDependencyDependedOnComponent.class);

                CyclicDependencyFoundException exception = assertThrows(CyclicDependencyFoundException.class, () ->
                        config.getContext());
                List<Class<?>> components = asList(exception.getComponents());

                assertEquals(3, components.size());
                assertTrue(components.contains(Component.class));
                assertTrue(components.contains(Dependency.class));
                assertTrue(components.contains(AnotherDependency.class));
            }

        }

        @Nested
        public class FieldInjectionTest {
            static class ComponentWithFieldInjection {
                @Inject
                Dependency dependency;
            }

            static class SubclassWithFieldInjection extends ComponentWithFieldInjection{}

            // inject field
            @Test
            @DisplayName("should inject dependency via field")
            public void should_inject_dependency_via_field() {
                Dependency dependency = new Dependency() {
                };
                config.bind(Dependency.class, dependency);
                config.bind(ComponentWithFieldInjection.class, ComponentWithFieldInjection.class);

                ComponentWithFieldInjection component = config.getContext().get(ComponentWithFieldInjection.class).get();
                assertSame(dependency, component.dependency);
            }

            @Test
            public void should_inject_dependency_via_superclass_inject_field() {
                Dependency dependency = new Dependency() {
                };
                config.bind(Dependency.class, dependency);
                config.bind(SubclassWithFieldInjection.class, SubclassWithFieldInjection.class);

                SubclassWithFieldInjection component = config.getContext().get(SubclassWithFieldInjection.class).get();
                assertSame(dependency, component.dependency);
            }

            @Test
            @DisplayName("should include field dependency in dependencies")
            public void should_include_field_dependency_in_dependencies() {
                ConstructorInjectionProvider<ComponentWithFieldInjection> provider = new ConstructorInjectionProvider<>(ComponentWithFieldInjection.class);
                assertArrayEquals(new Class<?>[]{Dependency.class}, provider.getDependencies().toArray(Class<?>[]::new));
            }

        }

        @Nested
        public class MethodInjectionTest {
        }

    }

    @Nested
    public class DependenciesSelectionTest {
    }

    @Nested
    public class LifecycleManagementTest {
    }

}

interface Component {

}

interface Dependency {
}

interface AnotherDependency {
}

class ComponentWithDefaultConstructor implements Component {

    public ComponentWithDefaultConstructor() {
    }

}

class ComponentWithInjectConstructor implements Component {
    private Dependency dependency;

    @Inject
    public ComponentWithInjectConstructor(Dependency dependency) {
        this.dependency = dependency;
    }

    public Dependency getDependency() {
        return dependency;
    }
}

class DependencyWithInjectConstructor implements Dependency {

    @Inject
    public DependencyWithInjectConstructor(String dependency) {
        this.dependency = dependency;
    }

    public String getDependency() {
        return dependency;
    }

    private String dependency;
}

class ComponentWithMultiInjectConstructors implements Component {
    @Inject
    public ComponentWithMultiInjectConstructors(String name) {
    }

    @Inject
    public ComponentWithMultiInjectConstructors(String name, double value) {
    }
}

class ComponentWithNoInjectConstructorNorDefaultConstructor implements Component {
    public ComponentWithNoInjectConstructorNorDefaultConstructor(String name) {
    }
}

class DependencyDependedOnComponent implements Dependency {
    public Component getComponent() {
        return component;
    }

    @Inject
    public DependencyDependedOnComponent(Component component) {
        this.component = component;
    }

    private Component component;
}

class AnotherDependencyDependedOnComponent implements AnotherDependency {
    public Component getComponent() {
        return component;
    }

    @Inject
    public AnotherDependencyDependedOnComponent(Component component) {
        this.component = component;
    }

    private Component component;
}

class DependencyDependedOnAnotherDependency implements Dependency {
    public AnotherDependency getAnotherDependency() {
        return anotherDependency;
    }

    @Inject
    public DependencyDependedOnAnotherDependency(AnotherDependency anotherDependency) {
        this.anotherDependency = anotherDependency;
    }

    private AnotherDependency anotherDependency;
}