import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.internal.util.collections.Sets;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
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
    public class DependencyCheckerTest {

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

        // component does not exist
        @Test
        public void should_return_empty_if_component_not_found() {
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

        static abstract class TypeLiteral<T> {
            public ParameterizedType getType() {
                return (ParameterizedType) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
            }
        }
    }

    @Nested
    public class DependenciesSelection {
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